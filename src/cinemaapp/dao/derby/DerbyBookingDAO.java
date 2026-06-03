package cinemaapp.dao.derby;

import cinemaapp.dao.BookingDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.AttendeeType;
import cinemaapp.model.Booking;
import cinemaapp.model.BookingItem;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Apache Derby implementation of {@link BookingDAO}.
 *
 * {@link #save} writes to both the Booking and BookingItem tables inside a
 * single transaction – either both writes succeed or both are rolled back,
 * preserving data integrity.
 */
public class DerbyBookingDAO implements BookingDAO {

    private final DatabaseManager dbManager;

    public DerbyBookingDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // -----------------------------------------------------------------------
    // Write
    // -----------------------------------------------------------------------

    /**
     * Persists a complete booking (header + all items) atomically.
     *
     * @param booking  the booking produced by the service layer
     * @param username the logged-in user who owns this booking
     */
    @Override
    public void save(Booking booking, String username) throws SQLException {
        Connection conn = dbManager.getConnection();
        conn.setAutoCommit(false);
        try {
            // 1. Insert Booking header
            String bookingSql =
                "INSERT INTO Booking (bookingCode, bookingDate, totalPrice, username) "
                + "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(bookingSql)) {
                ps.setString(1, booking.getBookingCode());
                ps.setDate(2, Date.valueOf(LocalDate.now()));
                ps.setDouble(3, booking.getTotalPrice());
                ps.setString(4, username);
                ps.executeUpdate();
            }

            // 2. Insert each BookingItem
            String itemSql =
                "INSERT INTO BookingItem (bookingCode, seatId, itemPrice, attendeeType) "
                + "VALUES (?, ?, ?, ?)";

            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (BookingItem item : booking.getBookingItems()) {
                    ps.setString(1, booking.getBookingCode());
                    ps.setString(2, item.getSeatId());
                    ps.setDouble(3, item.getItemPrice());
                    ps.setString(4, item.getAttendeeType().name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Deletes a booking and all its items transactionally.
     * Items must be deleted first to satisfy the FK constraint.
     */
    @Override
    public void delete(String bookingCode) throws SQLException {
        Connection conn = dbManager.getConnection();
        conn.setAutoCommit(false);
        try {
            // Delete items first (FK child)
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM BookingItem WHERE bookingCode = ?")) {
                ps.setString(1, bookingCode);
                ps.executeUpdate();
            }

            // Delete booking header (FK parent)
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM Booking WHERE bookingCode = ?")) {
                ps.setString(1, bookingCode);
                ps.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    @Override
    public Booking findByBookingCode(String bookingCode) throws SQLException {
        // Step 1: find the booking header (also retrieves showtimeId via BookingItem join)
        // The Booking model in P1 stores showtimeId; in the ERD showtimeId is embedded in
        // BookingItem indirectly via seatId → ShowSeat → showtimeId.  For simplicity we
        // store the showtimeId on the Booking model by reading it from the first BookingItem.
        String headerSql =
            "SELECT b.bookingCode, b.bookingDate, b.totalPrice, "
            + "       bi.seatId, bi.attendeeType, bi.itemPrice, "
            + "       ss.showtimeId "
            + "FROM Booking b "
            + "JOIN BookingItem bi ON b.bookingCode = bi.bookingCode "
            + "JOIN ShowSeat ss ON bi.seatId = ss.seatId "
            + "WHERE b.bookingCode = ?";

        Booking booking = null;

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(headerSql)) {
            ps.setString(1, bookingCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (booking == null) {
                        String showtimeId = rs.getString("showtimeId");
                        booking = new Booking(bookingCode, showtimeId);
                    }
                    String       seatId       = rs.getString("seatId");
                    AttendeeType attendeeType = AttendeeType.valueOf(rs.getString("attendeeType"));
                    double       itemPrice    = rs.getDouble("itemPrice");
                    booking.addBookingItem(new BookingItem(seatId, attendeeType, itemPrice));
                }
            }
        }

        if (booking != null) {
            booking.calculateTotalPrice();
        }
        return booking;
    }

    @Override
    public boolean existsByBookingCode(String bookingCode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Booking WHERE bookingCode = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, bookingCode);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    @Override
    public List<Booking> findByUsername(String username) throws SQLException {
        // Retrieve all booking codes for the user, then reconstruct each booking
        String codeSql = "SELECT bookingCode FROM Booking WHERE username = ? ORDER BY bookingDate DESC";
        List<String> codes = new ArrayList<>();

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(codeSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    codes.add(rs.getString("bookingCode"));
                }
            }
        }

        List<Booking> bookings = new ArrayList<>();
        for (String code : codes) {
            Booking b = findByBookingCode(code);
            if (b != null) {
                bookings.add(b);
            }
        }
        return bookings;
    }
}