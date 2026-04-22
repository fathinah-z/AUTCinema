package cinemaapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    /**
     * @return the showtimeId
     */
    private final String bookingCode;
    private final String showtimeId;
    private final List<BookingItem> bookingItems;
    private final LocalDateTime bookingDate;
    private double totalPrice;

    public Booking(String bookingCode, String showtimeId) {
        this.bookingCode = bookingCode;
        this.showtimeId = showtimeId;
        this.bookingItems = new ArrayList<>();
        this.bookingDate = LocalDateTime.now();
        this.totalPrice = 0.0;
    }

    public void addBookingItem(BookingItem item) {
        bookingItems.add(item);
    }

    public void calculateTotalPrice() {
        totalPrice = bookingItems.stream()
                .mapToDouble(BookingItem::getItemPrice)
                .sum();
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getShowtimeId() {
        return showtimeId;
    }

    public List<BookingItem> getBookingItems() {
        return bookingItems;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public double getTotalPrice() {
        return totalPrice;
    }
}
