package cinemaapp.model;

import java.time.LocalDateTime;

public class Showtime {

    private final String showtimeId;
    private final String movieId;
    private final String screenId;
    private LocalDateTime dateTime;
    private double basePrice;

    public Showtime(String showtimeId, String movieId, String screenId, LocalDateTime dateTime, double basePrice) {
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.screenId = screenId;
        this.dateTime = dateTime;
        this.basePrice = basePrice;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public String getScreenId() {
        return screenId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }
}
