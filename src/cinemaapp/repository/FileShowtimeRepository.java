package cinemaapp.repository;

import cinemaapp.model.Movie;
import cinemaapp.model.Showtime;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileShowtimeRepository implements ShowtimeRepository {

    private final List<Showtime> showtimes = new ArrayList<>();
    private final String filepath = "src/showtimes.txt";

    public FileShowtimeRepository() {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = br.readLine()) != null) {
                Showtime showtime = parseShowtime(line);
                showtimes.add(showtime);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read movies file", e);
        }
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

    private Showtime parseShowtime(String line) {
        String[] parts = line.split("\\|");

        String showtimeId = parts[0];
        String movieId = parts[1];
        String screenId = parts[2];
        LocalDateTime dateTime = LocalDateTime.parse(parts[3]);  // ISO-8601
        double basePrice = Double.parseDouble(parts[4]);

        return new Showtime(showtimeId, movieId, screenId, dateTime, basePrice);
    }

}
