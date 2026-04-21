package cinemaapp.repository;

import cinemaapp.model.SeatStatus;
import java.util.Map;

public interface ShowSeatRepository {
    void resetAllReservedSeats();
    boolean tryReserveSeat(String showtimeId, String seatId);
    boolean tryBookSeat(String showtimeId, String seatId);
    Map<String, SeatStatus> findByShowtimeId(String showtimeId);
    void updateSeatStatus(String showtimeId, String seatId, SeatStatus status);
}
