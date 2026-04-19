package cinemaapp.repository;

import cinemaapp.model.Showtime;
import java.util.List;

public interface ShowtimeRepository {
    Showtime findById(String showtimeId);
    List<Showtime> findByMovieId(String movieId);
}
