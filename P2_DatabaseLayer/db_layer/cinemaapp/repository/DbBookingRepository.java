package cinemaapp.repository;

import cinemaapp.dao.BookingDAO;
import cinemaapp.model.Booking;

import java.sql.SQLException;

/**
 * Adapts {@link BookingDAO} to the {@link BookingRepository} interface.
 *
 * The original {@link BookingRepository} interface does not carry a username
 * parameter on {@code saveBooking} because the CUI system had no login.
 * For Project 2 (with accounts) the GUI should call
 * {@link BookingDAO#save(Booking, String)} directly with the logged-in
 * username.  This adapter falls back to the guest username so the existing
 * service-layer code still compiles and runs during the integration period.
 *
 * TODO (Project 2): inject the current session username and pass it through.
 */
public class DbBookingRepository implements BookingRepository {

    private final BookingDAO bookingDAO;

    /** Username used when the GUI does not yet provide session context. */
    private String currentUsername = "guest";

    public DbBookingRepository(BookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    /** Call this after login so bookings are attributed to the right account. */
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }

    @Override
    public void saveBooking(Booking booking) {
        try {
            bookingDAO.save(booking, currentUsername);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – saveBooking: " + booking.getBookingCode(), e);
        }
    }

    @Override
    public void deleteBooking(Booking booking) {
        try {
            bookingDAO.delete(booking.getBookingCode());
        } catch (SQLException e) {
            throw new RuntimeException("DB error – deleteBooking: " + booking.getBookingCode(), e);
        }
    }

    @Override
    public Booking findByBookingCode(String bookingCode) {
        try {
            return bookingDAO.findByBookingCode(bookingCode);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findByBookingCode: " + bookingCode, e);
        }
    }

    @Override
    public boolean existsByBookingCode(String bookingCode) {
        try {
            return bookingDAO.existsByBookingCode(bookingCode);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – existsByBookingCode: " + bookingCode, e);
        }
    }
}
