package cinemaapp.dao.derby;

import cinemaapp.dao.ShowtimeDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.Showtime;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Apache Derby implementation of {@link ShowtimeDAO}.
 */
public class DerbyShowtimeDAO implements ShowtimeDAO {

    private final DatabaseManager dbManager;

    public DerbyShowtimeDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Showtime findById(String showtimeId) throws SQLException {
        String sql =
            "SELECT showtimeId, dateTime, basePrice, screenId, movieId "
            + "FROM Showtime WHERE showtimeId = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, showtimeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Showtime> findByMovieId(String movieId) throws SQLException {
        String sql =
            "SELECT showtimeId, dateTime, basePrice, screenId, movieId "
            + "FROM Showtime WHERE movieId = ? ORDER BY dateTime";

        List<Showtime> result = new ArrayList<>();
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        }
        return result;
    }

    @Override
    public List<Showtime> findAll() throws SQLException {
        String sql =
            "SELECT showtimeId, dateTime, basePrice, screenId, movieId "
            + "FROM Showtime ORDER BY dateTime";

        List<Showtime> result = new ArrayList<>();
        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        }
        return result;
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private Showtime mapRow(ResultSet rs) throws SQLException {
        // Derby stores TIMESTAMP; convert to LocalDateTime
        Timestamp ts = rs.getTimestamp("dateTime");
        LocalDateTime dateTime = ts.toLocalDateTime();

        return new Showtime(
            rs.getString("showtimeId"),
            rs.getString("movieId"),
            rs.getString("screenId"),
            dateTime,
            rs.getDouble("basePrice")
        );
    }
}
