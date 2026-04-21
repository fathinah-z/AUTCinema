package cinemaapp.service;

import cinemaapp.dto.MovieDetails;
import cinemaapp.dto.ShowInfo;
import cinemaapp.model.*;
import cinemaapp.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BrowsingService {

    private final MovieRepository movieRepo;
    private final ShowtimeRepository showtimeRepo;
    private final ShowSeatRepository showSeatRepo;

    public BrowsingService(MovieRepository movieRepo,
                           ShowtimeRepository showtimeRepo,
                           ShowSeatRepository showSeatRepo) {
        this.movieRepo = movieRepo;
        this.showtimeRepo = showtimeRepo;
        this.showSeatRepo = showSeatRepo;
    }

    public List<Movie> getMovies() {
        return movieRepo.findAll();
    }
    
    public Movie getMovieById(String movieId){
        return movieRepo.findById(movieId);
    }

    public MovieDetails getMovieDetails(String movieId) {
        Movie movie = movieRepo.findById(movieId);
        if (movie == null) return null;

        List<Showtime> showtimes = showtimeRepo.findByMovieId(movieId);
        List<ShowInfo> showInfoList = new ArrayList<>();

        for (Showtime showtime : showtimes) {
            Map<String, SeatStatus> seatMap = showSeatRepo.findByShowtimeId(showtime.getShowtimeId());
            long availCount = seatMap.values().stream()
                    .filter(status -> status == SeatStatus.AVAILABLE)
                    .count();
            showInfoList.add(new ShowInfo(showtime, (int) availCount));
        }

        return new MovieDetails(movie, showInfoList);
    }
}
