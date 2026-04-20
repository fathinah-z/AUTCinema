package cinemaapp.repository;

import cinemaapp.model.Booking;

public interface BookingRepository {
    void saveBooking(Booking booking);
    void deleteBooking(Booking booking);
    Booking findByBookingCode(String bookingCode);
    boolean existsByBookingCode(String bookingCode);
}
