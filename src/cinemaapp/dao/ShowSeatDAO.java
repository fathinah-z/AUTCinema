package cinemaapp.dao;

import cinemaapp.model.SeatStatus;
import java.sql.SQLException;
import java.util.Map;

/** Data-Access Object interface for ShowSeat. */
public interface ShowSeatDAO {
    /** Returns a map of seatId → SeatStatus for every seat in the showtime. */
    Map<String, SeatStatus> findByShowtimeId(String showtimeId) throws SQLException;

    /** Updates the status of one seat for a given showtime. */
    void updateSeatStatus(String showtimeId, String seatId, SeatStatus status) throws SQLException;

    /** Resets all RESERVED seats back to AVAILABLE (clears abandoned reservations). */
    void resetAllReservedSeats() throws SQLException;
}
