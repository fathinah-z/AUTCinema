package cinemaapp.model;

public class BookingItem {

    private String seatId;
    private AttendeeType attendeeType;
    private double itemPrice;

    public BookingItem(String seatId, AttendeeType attendeeType, double itemPrice) {
        this.seatId = seatId;
        this.attendeeType = attendeeType;
        this.itemPrice = itemPrice;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public AttendeeType getAttendeeType() {
        return attendeeType;
    }

    public void setAttendeeType(AttendeeType attendeeType) {
        this.attendeeType = attendeeType;
    }

    public double getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(double itemPrice) {
        this.itemPrice = itemPrice;
    }

    @Override
    public String toString() {
        return String.format("BookingItem[Seat:%s | %s | $%.2f]",
                seatId, attendeeType, itemPrice);
    }
}
