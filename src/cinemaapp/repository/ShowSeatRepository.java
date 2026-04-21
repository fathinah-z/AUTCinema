package cinemaapp.repository;

import cinemaapp.model.SeatStatus;
import java.util.Map;

public interface ShowSeatRepository {
    Map<String, SeatStatus> findByShowtimeId(String showtimeId);
    void updateSeatStatus(String showtimeId, String seatId, SeatStatus status);
}
