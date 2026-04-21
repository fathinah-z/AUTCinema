package cinemaapp.model;

public class ShowSeat {

    private String showtimeId;
    private String seatId;
    private SeatStatus seatStatus;

    public ShowSeat(String showtimeId, String seatId, SeatStatus seatStatus) {
        this.showtimeId = showtimeId;
        this.seatId = seatId;
        this.seatStatus = seatStatus;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public void setShowtimeId(String showtimeId) {
        this.showtimeId = showtimeId;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public SeatStatus getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(SeatStatus seatStatus) {
        this.seatStatus = seatStatus;
    }

    @Override
    public String toString() {
        return String.format("ShowSeat[Show:%s | Seat:%s | Status:%s]",
                showtimeId, seatId, seatStatus);
    }
}
