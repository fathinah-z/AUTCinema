package cinemaapp.repository;

import cinemaapp.dao.ShowSeatDAO;
import cinemaapp.model.SeatStatus;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

/** Adapts {@link ShowSeatDAO} to the {@link ShowSeatRepository} interface. */
public class DbShowSeatRepository implements ShowSeatRepository {

    private final ShowSeatDAO showSeatDAO;

    public DbShowSeatRepository(ShowSeatDAO showSeatDAO) {
        this.showSeatDAO = showSeatDAO;
    }

    @Override
    public Map<String, SeatStatus> findByShowtimeId(String showtimeId) {
        try {
            return showSeatDAO.findByShowtimeId(showtimeId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findByShowtimeId: " + showtimeId, e);
        }
    }

    @Override
    public void updateSeatStatus(String showtimeId, String seatId, SeatStatus status) {
        try {
            showSeatDAO.updateSeatStatus(showtimeId, seatId, status);
        } catch (SQLException e) {
            throw new RuntimeException(
                "DB error – updateSeatStatus showtime=" + showtimeId + " seat=" + seatId, e);
        }
    }

    @Override
    public void resetAllReservedSeats() {
        try {
            showSeatDAO.resetAllReservedSeats();
        } catch (SQLException e) {
            throw new RuntimeException("DB error – resetAllReservedSeats", e);
        }
    }
}
