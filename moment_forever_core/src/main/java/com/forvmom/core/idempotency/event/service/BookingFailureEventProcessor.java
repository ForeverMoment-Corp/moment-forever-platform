package com.forvmom.core.idempotency.event.service;

import com.forvmom.common.dto.events.BookingCancelledEvent;
import com.forvmom.common.dto.events.BookingFailedEvent;
import com.forvmom.common.dto.events.InventoryReleaseEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.idempotency.event.model.ProducerEventIdentity;
import com.forvmom.core.services.BookingReservationService;
import com.forvmom.data.dao.ConsumerEventInboxDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Applies booking-failure and booking-cancellation events exactly once at the
 * business-effect boundary.
 *
 * <p>Modern events are deduplicated by producer and event ID in the durable
 * inbox. Legacy events without either identity remain supported and rely on
 * the reservation service's release-once guard.
 *
 * <p>Both event kinds share one pipeline ({@code processRelease}): inbox
 * claim → reservation CAS release → inbox mark-processed. The only difference
 * is the event type carried for logging and inbox bookkeeping.
 */
@Service
public class BookingFailureEventProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(BookingFailureEventProcessor.class);

    private final ConsumerEventInboxDao consumerEventInboxDao;
    private final BookingReservationService bookingReservationService;

    public BookingFailureEventProcessor(
            ConsumerEventInboxDao consumerEventInboxDao,
            BookingReservationService bookingReservationService
    ) {
        this.consumerEventInboxDao = consumerEventInboxDao;
        this.bookingReservationService = bookingReservationService;
    }

    /**
     * Claims the event identity, releases reserved capacity once, and marks the
     * inbox delivery processed in one transaction.
     */
    @Transactional
    public EventProcessingResult process(BookingFailedEvent event) {
        return processRelease(event, "failure");
    }

    /**
     * Cancellation twin of {@link #process(BookingFailedEvent)}: same inbox
     * dedup, same reservation CAS release, same exactly-once guarantees.
     * A cancellation for an already-released reservation (e.g. a FAILED
     * booking cancelled by mistake — rejected upstream, but safe anyway) is a
     * BUSINESS_NO_OP, never a double release.
     */
    @Transactional
    public EventProcessingResult process(BookingCancelledEvent event) {
        return processRelease(event, "cancellation");
    }

    private EventProcessingResult processRelease(InventoryReleaseEvent event, String kind) {
        ProducerEventIdentity eventIdentity = validateIdentity(event);
        if (eventIdentity == null) {
            logger.debug(
                    "Processing legacy booking {} without event identity: bookingId={}",
                    kind, event.getBookingId());
            return releaseReservation(event)
                    ? EventProcessingResult.PROCESSED
                    : EventProcessingResult.BUSINESS_NO_OP;
        }

        int claimCount = consumerEventInboxDao.claim(
                eventIdentity.producer(),
                eventIdentity.eventId(),
                event.getEventType(),
                event.getBookingId(),
                event.getCorrelationId());
        if (claimCount == 0) {
            logger.debug(
                    "Ignored duplicate booking {} event: producer={}, eventId={}",
                    kind, eventIdentity.producer(),
                    eventIdentity.eventId());
            return EventProcessingResult.DUPLICATE_EVENT;
        }

        /*
         * The inbox identifies a delivery; the reservation state protects the
         * business effect. Distinct events remain processable while capacity
         * can still be released only once.
         */
        boolean released = releaseReservation(event);
        int processedCount = consumerEventInboxDao.markProcessed(
                eventIdentity.producer(),
                eventIdentity.eventId(),
                LocalDateTime.now());
        if (processedCount != 1) {
            throw new IllegalStateException(
                    "Could not mark consumer event as processed: producer="
                            + eventIdentity.producer() + ", eventId=" + eventIdentity.eventId());
        }
        return released ? EventProcessingResult.PROCESSED : EventProcessingResult.BUSINESS_NO_OP;
    }

    /**
     * Normalizes the compound event identity and rejects partially identified events.
     */
    private ProducerEventIdentity validateIdentity(InventoryReleaseEvent event) {
        String producer = normalize(event.getProducer());
        String eventId = normalize(event.getEventId());
        if (producer == null && eventId == null) {
            return null;
        }
        if (producer == null || eventId == null) {
            throw new IllegalArgumentException(
                    "Inventory-release event producer and eventId must either both be present or both be absent");
        }
        return new ProducerEventIdentity(producer, eventId);
    }

    /**
     * Converts blank identity components to {@code null}.
     */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Delegates to the reservation-level idempotent capacity release.
     */
    private boolean releaseReservation(InventoryReleaseEvent event) {
        return bookingReservationService.releaseOnce(
                event.getBookingId(),
                event.getTimeSlotMapperId(),
                event.getBookingDate(),
                event.getGuestCount());
    }

}
