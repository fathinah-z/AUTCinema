package cinemaapp.dao.derby;

import cinemaapp.dao.ScreenDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.Screen;
import cinemaapp.model.Seat;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Apache Derby implementation of {@link ScreenDAO}.
 *
 * Screens are stored as a single row in the Screen table; their seats are
 * fetched from the Seat table and assembled into the {@link Screen} object's
 * seating layout here, keeping the model objects unchanged from Project 1.
 */
public class DerbyScreenDAO implements ScreenDAO {

    private final DatabaseManager dbManager;

    public DerbyScreenDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Screen findById(String screenId) throws SQLException {
        // Verify the screen exists
        String screenSql = "SELECT screenId FROM Screen WHERE screenId = ?";
        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(screenSql)) {
            ps.setString(1, screenId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // Screen not found
                }
            }
        }

        // Fetch all seats for this screen, ordered for deterministic layout
        List<Seat> seats = new ArrayList<>();
        char firstRow = 'Z';
        char lastRow  = 'A';
        int  seatsPerRow = 0;

        String seatSql =
            "SELECT seatId, row, number, nearAisle, isAccessible "
            + "FROM Seat WHERE screenId = ? ORDER BY row, number";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(seatSql)) {
            ps.setString(1, screenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String seatId    = rs.getString("seatId");
                    char   row       = rs.getString("row").charAt(0);
                    int    number    = rs.getInt("number");
                    boolean nearAisle   = rs.getInt("nearAisle") == 1;
                    boolean accessible  = rs.getInt("isAccessible") == 1;

                    seats.add(new Seat(seatId, row, number, nearAisle, accessible));

                    // Derive firstRow / lastRow / seatsPerRow from the data
                    if (row < firstRow) firstRow = row;
                    if (row > lastRow)  lastRow  = row;
                    if (number > seatsPerRow) seatsPerRow = number;
                }
            }
        }

        if (seats.isEmpty()) {
            // Screen exists but has no seats – return minimal object
            return new Screen(screenId, 'A', 'A', 0, seats);
        }

        return new Screen(screenId, firstRow, lastRow, seatsPerRow, seats);
    }
}
