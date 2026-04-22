package cinemaapp.dto;

import cinemaapp.model.Movie;
import java.util.List;

public class MovieDetails {
    private Movie movie;
    private List<ShowInfo> showtimes;

    public MovieDetails(Movie movie, List<ShowInfo> showtimes) {
        this.movie = movie;
        this.showtimes = showtimes;
    }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public List<ShowInfo> getShowtimes() { return showtimes; }
    public void setShowtimes(List<ShowInfo> showtimes) { this.showtimes = showtimes; }
}
