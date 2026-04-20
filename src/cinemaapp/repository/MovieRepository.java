package cinemaapp.repository;

import cinemaapp.model.Movie;
import java.util.List;

public interface MovieRepository {
    List<Movie> findAll();
    Movie findById(String movieId);
}
