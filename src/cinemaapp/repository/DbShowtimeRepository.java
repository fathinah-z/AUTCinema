package cinemaapp.repository;

import cinemaapp.dao.ShowtimeDAO;
import cinemaapp.model.Showtime;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/** Adapts {@link ShowtimeDAO} to the {@link ShowtimeRepository} interface. */
public class DbShowtimeRepository implements ShowtimeRepository {

    private final ShowtimeDAO showtimeDAO;

    public DbShowtimeRepository(ShowtimeDAO showtimeDAO) {
        this.showtimeDAO = showtimeDAO;
    }

    @Override
    public Showtime findById(String showtimeId) {
        try {
            return showtimeDAO.findById(showtimeId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findById showtime: " + showtimeId, e);
        }
    }

    @Override
    public List<Showtime> findByMovieId(String movieId) {
        try {
            return showtimeDAO.findByMovieId(movieId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findByMovieId: " + movieId, e);
        }
    }
}
