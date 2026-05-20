package cinemaapp.dao.derby;

import cinemaapp.dao.MovieDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.Movie;
import cinemaapp.model.MovieRating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Apache Derby implementation of {@link MovieDAO}.
 *
 * All SQL is parameterised via {@link PreparedStatement} to prevent injection.
 * The class itself is package-private; callers depend only on the {@link MovieDAO}
 * interface, following the Dependency Inversion Principle.
 */
public class DerbyMovieDAO implements MovieDAO {

    private final DatabaseManager dbManager;

    public DerbyMovieDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // -----------------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------------

    @Override
    public List<Movie> findAll() throws SQLException {
        List<Movie> movies = new ArrayList<>();
        String sql = "SELECT movieId, title, rating, description, runtime FROM Movie ORDER BY title";

        try (Statement st = dbManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                movies.add(mapRow(rs));
            }
        }
        return movies;
    }

    @Override
    public Movie findById(String movieId) throws SQLException {
        String sql = "SELECT movieId, title, rating, description, runtime FROM Movie WHERE movieId = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, movieId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // -----------------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------------

    @Override
    public void save(Movie movie) throws SQLException {
        String sql = "INSERT INTO Movie (movieId, title, rating, description, runtime) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, movie.getMovieId());
            ps.setString(2, movie.getTitle());
            ps.setString(3, movie.getRating().name());
            ps.setString(4, movie.getDescription());
            ps.setInt(5, movie.getRuntime());
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Movie movie) throws SQLException {
        String sql = "UPDATE Movie SET title=?, rating=?, description=?, runtime=? WHERE movieId=?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, movie.getTitle());
            ps.setString(2, movie.getRating().name());
            ps.setString(3, movie.getDescription());
            ps.setInt(4, movie.getRuntime());
            ps.setString(5, movie.getMovieId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String movieId) throws SQLException {
        String sql = "DELETE FROM Movie WHERE movieId = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, movieId);
            ps.executeUpdate();
        }
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private Movie mapRow(ResultSet rs) throws SQLException {
        return new Movie(
            rs.getString("movieId"),
            rs.getString("title"),
            MovieRating.valueOf(rs.getString("rating")),
            rs.getString("description"),
            rs.getInt("runtime")
        );
    }
}
