package cinemaapp.model;

public class ShowSeat {

    private final String showtimeId;
    private final String seatId;
    private SeatStatus seatStatus;

    public ShowSeat(String showtimeId, String seatId, SeatStatus seatStatus) {
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.seatStatus = seatStatus;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public String getSeatId() {
        return seatId;
    }

    public SeatStatus getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(SeatStatus seatStatus) {
        this.seatStatus = seatStatus;
    }
}
