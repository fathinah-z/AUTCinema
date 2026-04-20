package cinemaapp.repository;

import cinemaapp.model.Booking;

public interface BookingRepository {
    void saveBooking(Booking booking);
    Booking findByBookingCode(String bookingCode);
    void deleteBooking(Booking booking);
}
