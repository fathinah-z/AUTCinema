package cinemaapp.service;

import cinemaapp.model.*;
import cinemaapp.repository.*;

import java.time.LocalDateTime;

public class CancelBookingService {

    private final BookingRepository bookingRepo;
    private final ShowSeatRepository showSeatRepo;
    private final ShowtimeRepository showtimeRepo;

    public CancelBookingService(BookingRepository bookingRepo,
                                ShowSeatRepository showSeatRepo,
                                ShowtimeRepository showtimeRepo) {
        this.bookingRepo = bookingRepo;
        this.showSeatRepo = showSeatRepo;
        this.showtimeRepo = showtimeRepo;
    }
    
    public Booking findBookingByCode(String code) {
        return bookingRepo.findByBookingCode(code);
    }

    public Showtime getShowtimeById(String showtimeId) {
        return showtimeRepo.findById(showtimeId);
    }
    
    public boolean cancelBooking(Booking booking) {
        if (booking == null) return false;

        // Release all seats back to AVAILABLE
        for (BookingItem item : booking.getBookingItems()) {
            showSeatRepo.updateSeatStatus(booking.getShowtimeId(), item.getSeatId(), SeatStatus.AVAILABLE);
        }

        bookingRepo.deleteBooking(booking);
        return true;
    }

    public boolean isRefundEligible(Booking booking, LocalDateTime now) {
        if (booking == null) return false;

        Showtime showtime = showtimeRepo.findById(booking.getShowtimeId());
        if (showtime == null) return false;

        LocalDateTime showtimeDateTime = showtime.getDateTime();

        // Refund is eligible if showtime is at least 5 days away
        return now.plusDays(5).isBefore(showtimeDateTime);
    }
}
