package com.forvmom.common.dto.events;

import java.time.LocalDate;

/**
 * Contract for booking-lifecycle events that release reserved inventory on
 * the platform side (failures today, cancellations now).
 *
 * <p>Implemented by {@link BookingFailedEvent} and
 * {@link BookingCancelledEvent} so the inbox-dedup + release-once pipeline
 * ({@code BookingFailureEventProcessor}) is written once against this
 * interface instead of once per event type.
 */
public interface InventoryReleaseEvent {

    String getProducer();

    String getEventId();

    String getEventType();

    String getBookingId();

    String getCorrelationId();

    Long getTimeSlotMapperId();

    LocalDate getBookingDate();

    Integer getGuestCount();
}
