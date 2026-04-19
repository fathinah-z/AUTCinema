package cinemaapp.repository;

import cinemaapp.model.Movie;
import cinemaapp.model.MovieRating;
import java.util.ArrayList;
import java.util.List;

public class FileMovieRepository implements MovieRepository {

    private final List<Movie> movies = new ArrayList<>();

    public FileMovieRepository() {
        // Seed sample data — replace with file I/O as needed
        movies.add(new Movie("M001", "Galactic Drift", MovieRating.PG,
                "An epic space adventure across the galaxy.", 132));
        movies.add(new Movie("M002", "Shadow Protocol", MovieRating.M,
                "A spy thriller with unexpected twists.", 118));
        movies.add(new Movie("M003", "Little Wonders", MovieRating.G,
                "A heartwarming animated family film.", 95));
        movies.add(new Movie("M004", "Crimson Edge", MovieRating.R18,
                "An intense action film not for the faint-hearted.", 140));
    }

    @Override
    public List<Movie> findAll() {
        return new ArrayList<>(movies);
    }

    @Override
    public Movie findById(String movieId) {
        return movies.stream()
                .filter(m -> m.getMovieId().equals(movieId))
                .findFirst()
                .orElse(null);
    }
}
