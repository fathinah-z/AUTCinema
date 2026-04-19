package cinemaapp.repository;

import cinemaapp.model.Booking;
import java.util.ArrayList;
import java.util.List;

public class FileBookingRepository implements BookingRepository {

    private final List<Booking> bookings = new ArrayList<>();

    @Override
    public void saveBooking(Booking booking) {
        bookings.add(booking);
    }

    @Override
    public Booking findByBookingCode(String bookingCode) {
        return bookings.stream()
                .filter(b -> b.getBookingCode().equals(bookingCode))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteBooking(Booking booking) {
        bookings.remove(booking);
    }
}
