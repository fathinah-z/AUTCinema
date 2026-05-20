package cinemaapp.dao.derby;

import cinemaapp.dao.ShowSeatDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.SeatStatus;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Apache Derby implementation of {@link ShowSeatDAO}.
 *
 * ShowSeat has a composite primary key (seatId, showtimeId), so
 * {@code UPDATE} is safe without risk of duplicate rows.
 */
public class DerbyShowSeatDAO implements ShowSeatDAO {

    private final DatabaseManager dbManager;

    public DerbyShowSeatDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Map<String, SeatStatus> findByShowtimeId(String showtimeId) throws SQLException {
        Map<String, SeatStatus> result = new LinkedHashMap<>();
        String sql =
            "SELECT ss.seatId, ss.seatStatus "
            + "FROM ShowSeat ss "
            + "JOIN Seat s ON ss.seatId = s.seatId "
            + "WHERE ss.showtimeId = ? "
            + "ORDER BY s.row, s.number";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String     seatId = rs.getString("seatId");
                    SeatStatus status = SeatStatus.valueOf(rs.getString("seatStatus"));
                    result.put(seatId, status);
                }
            }
        }
        return result;
    }

    @Override
    public void updateSeatStatus(String showtimeId, String seatId, SeatStatus status)
            throws SQLException {
        String sql =
            "UPDATE ShowSeat SET seatStatus = ? WHERE showtimeId = ? AND seatId = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, showtimeId);
            ps.setString(3, seatId);
            ps.executeUpdate();
        }
    }

    @Override
    public void resetAllReservedSeats() throws SQLException {
        String sql = "UPDATE ShowSeat SET seatStatus = 'AVAILABLE' WHERE seatStatus = 'RESERVED'";

        try (Statement st = dbManager.getConnection().createStatement()) {
            int rows = st.executeUpdate(sql);
            if (rows > 0) {
                System.out.println("Reset " + rows + " abandoned reserved seat(s) to AVAILABLE.");
            }
        }
    }
}
