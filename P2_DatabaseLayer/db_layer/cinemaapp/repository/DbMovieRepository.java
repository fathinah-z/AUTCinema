package cinemaapp.repository;

import cinemaapp.dao.MovieDAO;
import cinemaapp.model.Movie;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * Adapts {@link MovieDAO} to the {@link MovieRepository} interface
 * used by the existing service layer.
 *
 * This is the Adapter pattern: the service layer calls a Repository interface
 * it already knows; this class translates those calls to DAO operations,
 * wrapping checked {@link SQLException} into unchecked
 * {@link RuntimeException} so the service layer code is unchanged.
 */
public class DbMovieRepository implements MovieRepository {

    private final MovieDAO movieDAO;

    public DbMovieRepository(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    @Override
    public List<Movie> findAll() {
        try {
            return movieDAO.findAll();
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findAll movies", e);
        }
    }

    @Override
    public Movie findById(String movieId) {
        try {
            return movieDAO.findById(movieId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findById movie: " + movieId, e);
        }
    }
}
