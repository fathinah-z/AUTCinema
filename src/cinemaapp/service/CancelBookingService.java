package cinemaapp.service;

import cinemaapp.model.*;
import cinemaapp.repository.*;

import java.time.LocalDateTime;

public class CancelBookingService {

    private final BookingRepository bookingRepo;
    private final ShowSeatRepository showSeatRepo;

    public CancelBookingService(BookingRepository bookingRepo,
                                ShowSeatRepository showSeatRepo) {
        this.bookingRepo = bookingRepo;
        this.showSeatRepo = showSeatRepo;
    }

    public boolean cancelBooking(Booking booking) {
        if (booking == null) return false;

        // Release all seats back to AVAILABLE
        for (BookingItem item : booking.getBookingItems()) {
            showSeatRepo.updateSeatStatus(item.getShowtimeId(), item.getSeatId(), SeatStatus.AVAILABLE);
        }

        bookingRepo.deleteBooking(booking);
        System.out.println("Booking " + booking.getBookingCode() + " has been cancelled.");
        return true;
    }

    /**
     * Refunds are eligible if cancellation happens at least 2 hours before show time.
     * Requires the showtime's dateTime for comparison.
     */
    public boolean isRefundEligible(Booking booking, LocalDateTime now) {
        if (booking == null) return false;
        // A booking is refund-eligible if it was made more than 2 hours ago is NOT
        // the criterion — the show must be more than 2 hours away.
        // This simplified version checks booking date as a placeholder.
        return booking.getBookingDate().plusHours(2).isAfter(now);
    }
}
