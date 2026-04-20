package cinemaapp.dto;

import cinemaapp.model.Showtime;

public class ShowInfo {
    private Showtime showtime;
    private int availSeats;

    public ShowInfo(Showtime showtime, int availSeats) {
        this.showtime = showtime;
        this.availSeats = availSeats;
    }

    public Showtime getShowtime() { return showtime; }
    public void setShowtime(Showtime showtime) { this.showtime = showtime; }

    public int getAvailSeats() { return availSeats; }
    public void setAvailSeats(int availSeats) { this.availSeats = availSeats; }

    @Override
    public String toString() {
        return String.format("ShowInfo[%s | Available Seats:%d]", showtime, availSeats);
    }
}
