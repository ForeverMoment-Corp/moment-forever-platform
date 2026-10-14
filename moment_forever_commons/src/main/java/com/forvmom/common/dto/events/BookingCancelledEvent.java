package com.forvmom.common.dto.events;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Published to topic {@code booking-cancelled} by the Booking Service when a
 * PENDING or CONFIRMED booking is cancelled (admin or owner-initiated).
 *
 * <p>
 * Consumed by Core Service to release the date-scoped inventory reserved during
 * the booking request transaction — the same release path as
 * {@link BookingFailedEvent}.
 *
 * <p>
 * IMPORTANT: {@code timeSlotMapperId}, {@code bookingDate}, and
 * {@code guestCount} identify the inventory row and quantity to release.
 * Payment reversal is a manual ops process; this event carries no payment data.
 */
public class BookingCancelledEvent extends BaseEvent implements InventoryReleaseEvent {

    private String bookingId;
    private Long userId;
    private String userEmail;
    private Long experienceId;
    /** Required for Core to identify the configured slot mapper. */
    private Long timeSlotMapperId;
    /** Required for Core to know how much capacity to release. */
    private Integer guestCount;
    private LocalDate bookingDate;
    private String cancelReason;
    private LocalDateTime cancelledAt;

    public BookingCancelledEvent() {
        super();
        setEventType("BOOKING_CANCELLED");
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(Long experienceId) {
        this.experienceId = experienceId;
    }

    public Long getTimeSlotMapperId() {
        return timeSlotMapperId;
    }

    public void setTimeSlotMapperId(Long timeSlotMapperId) {
        this.timeSlotMapperId = timeSlotMapperId;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(Integer guestCount) {
        this.guestCount = guestCount;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
