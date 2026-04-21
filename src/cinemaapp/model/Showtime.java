package cinemaapp.model;

import java.time.LocalDateTime;

public class Showtime {

    private String showtimeId;
    private LocalDateTime dateTime;
    private String movieId;
    private String screenId;
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

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getScreenId() {
        return screenId;
    }

    public void setScreenId(String screenId) {
        this.screenId = screenId;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
    }

    @Override
    public String toString() {
        return String.format("Showtime[%s | Movie:%s | %s | $%.2f]",
                showtimeId, movieId, dateTime, basePrice);
    }
}
