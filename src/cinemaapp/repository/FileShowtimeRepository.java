package cinemaapp.repository;

import cinemaapp.model.Showtime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileShowtimeRepository implements ShowtimeRepository {

    private final List<Showtime> showtimes = new ArrayList<>();

    public FileShowtimeRepository() {
        // Seed sample data — replace with file I/O as needed
        showtimes.add(new Showtime("ST001", LocalDateTime.of(2026, 4, 20, 10, 0), "M001", "SC001", 14.50));
        showtimes.add(new Showtime("ST002", LocalDateTime.of(2026, 4, 20, 13, 30), "M001", "SC002", 14.50));
        showtimes.add(new Showtime("ST003", LocalDateTime.of(2026, 4, 20, 16, 0), "M002", "SC001", 12.00));
        showtimes.add(new Showtime("ST004", LocalDateTime.of(2026, 4, 21, 18, 0), "M002", "SC003", 16.00));
        showtimes.add(new Showtime("ST005", LocalDateTime.of(2026, 4, 21, 20, 30), "M003", "SC002", 18.00));
    }

    @Override
    public Showtime findById(String showtimeId) {
        return showtimes.stream()
                .filter(s -> s.getShowtimeId().equals(showtimeId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<Showtime> findByMovieId(String movieId) {
        List<Showtime> result = new ArrayList<>();
        for (Showtime s : showtimes) {
            if (s.getMovieId().equals(movieId)) {
                result.add(s);
            }
        }
        return result;
    }
}
