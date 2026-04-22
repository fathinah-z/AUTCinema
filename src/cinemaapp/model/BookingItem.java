package cinemaapp.model;

public class BookingItem {

    private final String seatId;
    private final AttendeeType attendeeType;
    private final double itemPrice;

    public BookingItem(String seatId, AttendeeType attendeeType, double itemPrice) {
        this.seatId = seatId;
        this.attendeeType = attendeeType;
        this.itemPrice = itemPrice;
    }

    public String getSeatId() {
        return seatId;
    }

    public AttendeeType getAttendeeType() {
        return attendeeType;
    }

    public double getItemPrice() {
        return itemPrice;
    }
}
