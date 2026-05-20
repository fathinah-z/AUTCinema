package cinemaapp.dao;

import cinemaapp.model.Showtime;
import java.sql.SQLException;
import java.util.List;

/** Data-Access Object interface for {@link Showtime}. */
public interface ShowtimeDAO {
    Showtime findById(String showtimeId) throws SQLException;
    List<Showtime> findByMovieId(String movieId) throws SQLException;
    List<Showtime> findAll() throws SQLException;
}
