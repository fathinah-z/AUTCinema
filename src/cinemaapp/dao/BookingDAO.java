package cinemaapp.dao;

import cinemaapp.model.Booking;
import java.sql.SQLException;
import java.util.List;

/** Data-Access Object interface for {@link Booking}. */
public interface BookingDAO {
    void save(Booking booking, String username) throws SQLException;
    void delete(String bookingCode) throws SQLException;
    Booking findByBookingCode(String bookingCode) throws SQLException;
    boolean existsByBookingCode(String bookingCode) throws SQLException;
    List<Booking> findByUsername(String username) throws SQLException;
}
