package cinemaapp.repository;

import cinemaapp.model.SeatStatus;
import java.util.HashMap;

public interface ShowSeatRepository {
    HashMap<String, SeatStatus> findByShowtimeId(String showtimeId);
    void updateSeatStatus(String showtimeId, String seatId, SeatStatus status);
}
