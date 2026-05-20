package cinemaapp.dao;

import cinemaapp.model.Movie;
import java.sql.SQLException;
import java.util.List;

/**
 * Data-Access Object interface for {@link Movie}.
 *
 * Keeping a clean interface here allows the service layer to depend on the
 * abstraction, not the Derby implementation – supporting the Dependency
 * Inversion Principle and making unit-testing with mocks straightforward.
 */
public interface MovieDAO {
    List<Movie> findAll() throws SQLException;
    Movie findById(String movieId) throws SQLException;
    void save(Movie movie) throws SQLException;
    void update(Movie movie) throws SQLException;
    void delete(String movieId) throws SQLException;
}
