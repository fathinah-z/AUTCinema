package cinemaapp.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {
    private String bookingCode;
    private List<BookingItem> bookingItems;
    private LocalDateTime bookingDate;
    private double totalPrice;

    public Booking(String bookingCode) {
        this.bookingCode = bookingCode;
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

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public List<BookingItem> getBookingItems() { return bookingItems; }
    public void setBookingItems(List<BookingItem> bookingItems) { this.bookingItems = bookingItems; }

    public LocalDateTime getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDateTime bookingDate) { this.bookingDate = bookingDate; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    @Override
    public String toString() {
        return String.format("Booking[Code:%s | Items:%d | Total:$%.2f | Date:%s]",
                bookingCode, bookingItems.size(), totalPrice, bookingDate);
    }
}
