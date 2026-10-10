package com.forvmom.core.event_enrichment;

import com.forvmom.common.dto.snapshot.ExperienceSnapshot;
import com.forvmom.common.dto.snapshot.LocationSnapshot;
import com.forvmom.common.dto.snapshot.SlotSnapshot;
import com.forvmom.common.dto.snapshot.UserSnapshot;
import com.forvmom.common.helpers.BookingOutboxPayload;
import com.forvmom.common.helpers.BookingPricingSummary;
import com.forvmom.common.helpers.BookingSnapshotBundle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingPricingCalculatorTest {

    private final BookingPricingCalculator calculator = new BookingPricingCalculator();

    private BookingOutboxPayload payload(BigDecimal requestedAmount) {
        return new BookingOutboxPayload.Builder()
                .withUserId(1L)
                .withSlotMapperId(4L)
                .withGuestCount(2)
                .withBookingDate(LocalDate.now().plusDays(1))
                .withRequestedAmount(requestedAmount)
                .build();
    }

    private BookingSnapshotBundle snapshots() {
        return new BookingSnapshotBundle.Builder()
                .withUserSnapshot(new UserSnapshot(1L, "a@b.c", "Test"))
                .withExperienceSnapshot(new ExperienceSnapshot(1L, "Exp", "exp", new BigDecimal("100.00")))
                .withLocationSnapshot(new LocationSnapshot(2L, "Loc", new BigDecimal("8.00")))
                .withSlotSnapshot(new SlotSnapshot(4L, 1L, 2L, 1L, "t1", "14:00", "15:00",
                        new BigDecimal("8.00"), 20))
                .withAddonSnapshots(Collections.emptyList())
                .build();
    }

    @Test
    void derivedPricingUsedWhenNoClientAmount() {
        BookingPricingSummary summary = calculator.calculate(payload(null), snapshots());

        assertEquals(new BigDecimal("8.00"), summary.getResolvedPricePerPerson());
        assertEquals("SLOT", summary.getPricingLevel());
        assertEquals(new BigDecimal("16.00"), summary.getTotalAmount());
        assertEquals(new BigDecimal("16.00"), summary.getGrandTotal());
    }

    @Test
    void clientAmountOverridesGrandTotal() {
        BookingPricingSummary summary =
                calculator.calculate(payload(new BigDecimal("20.00")), snapshots());

        assertEquals(new BigDecimal("20.00"), summary.getGrandTotal());
        assertEquals("CLIENT", summary.getPricingLevel());
        // Derived breakdown is preserved for audit.
        assertEquals(new BigDecimal("8.00"), summary.getResolvedPricePerPerson());
        assertEquals(new BigDecimal("16.00"), summary.getTotalAmount());
    }
}
