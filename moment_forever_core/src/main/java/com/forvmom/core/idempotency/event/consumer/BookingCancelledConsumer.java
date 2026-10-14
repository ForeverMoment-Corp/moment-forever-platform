package com.forvmom.core.idempotency.event.consumer;

import com.forvmom.common.dto.events.BookingCancelledEvent;
import com.forvmom.core.idempotency.event.model.EventProcessingResult;
import com.forvmom.core.idempotency.event.service.BookingFailureEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Core service consumer for the {@code booking-cancelled} topic.
 *
 * <p>When a booking is cancelled (admin on the customer's behalf, or the
 * owner), the Booking Service publishes a {@link BookingCancelledEvent}. This
 * adapter reuses the same transactional inbox claim + idempotent inventory
 * release pipeline as {@code booking-failed} (see
 * {@link BookingFailureEventProcessor#process(BookingCancelledEvent)}), then
 * acknowledges the Kafka delivery only after that work succeeds.
 */
@Component
public class BookingCancelledConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingCancelledConsumer.class);

    private final BookingFailureEventProcessor bookingFailureEventProcessor;

    public BookingCancelledConsumer(BookingFailureEventProcessor bookingFailureEventProcessor) {
        this.bookingFailureEventProcessor = bookingFailureEventProcessor;
    }

    /**
     * Processes one booking-cancellation delivery and acknowledges it only after the
     * inbox and reservation transaction commits successfully.
     */
    @KafkaListener(topics = "${kafka.topics.booking-cancelled}", groupId = "core-booking-cancelled-group", containerFactory = "kafkaListenerContainerFactory")
    public void onBookingCancelled(
            @Payload BookingCancelledEvent event,
            Acknowledgment acknowledgment
    ) {
        logger.info(
                "Received booking-cancelled: bookingId={}, eventId={}, correlationId={}, slotMapperId={}, bookingDate={}, guestCount={}",
                event.getBookingId(), event.getEventId(), event.getCorrelationId(),
                event.getTimeSlotMapperId(), event.getBookingDate(), event.getGuestCount());

        try {
            EventProcessingResult result =
                    bookingFailureEventProcessor.process(event);

            logger.info(
                    "Booking cancellation handled: bookingId={}, producer={}, eventId={}, result={}, slotMapperId={}, bookingDate={}",
                    event.getBookingId(), event.getProducer(), event.getEventId(), result,
                    event.getTimeSlotMapperId(), event.getBookingDate());

            acknowledgment.acknowledge();

        } catch (Exception exception) {
            logger.error("Failed to rollback inventory for cancelled bookingId={}: {}",
                    event.getBookingId(), exception.getMessage(), exception);
            // Do NOT ack — Kafka will redeliver → DLQ after max retries
            throw exception;
        }
    }
}
