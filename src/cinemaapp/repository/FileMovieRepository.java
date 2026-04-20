package cinemaapp.repository;

import cinemaapp.model.Movie;
import cinemaapp.model.MovieRating;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileMovieRepository implements MovieRepository {

    private final List<Movie> movies = new ArrayList<>();
    private final String filepath = "src/movies.txt";

    public FileMovieRepository() {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = br.readLine()) != null) {
                Movie movie = parseMovie(line);
                movies.add(movie);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read movies file", e);
        }
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

    private Movie parseMovie(String line) {
        String[] parts = line.split("\\|");

        String movieId = parts[0];
        String title = parts[1];
        MovieRating rating = MovieRating.valueOf(parts[2]);   // enum conversion
        String description = parts[3];
        int runtime = Integer.parseInt(parts[4]);

        return new Movie(movieId, title, rating, description, runtime);
    }
}
