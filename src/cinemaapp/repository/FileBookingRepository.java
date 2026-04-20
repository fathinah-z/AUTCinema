package cinemaapp.repository;

import cinemaapp.model.AttendeeType;
import cinemaapp.model.Booking;
import cinemaapp.model.BookingItem;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new LinkedHashMap<>();
    private final String filepath = "src/bookings.txt";

    public FileBookingRepository() {
        load();
    }

    @Override
    public void saveBooking(Booking booking) {
        bookings.put(booking.getBookingCode(), booking);
        saveToFile();
    }

    @Override
    public void deleteBooking(Booking booking) {
        bookings.remove(booking.getBookingCode());
        saveToFile();
    }

    @Override
    public Booking findByBookingCode(String bookingCode) {
        return bookings.get(bookingCode);
    }

    @Override
    public boolean existsByBookingCode(String bookingCode) {
        return bookings.containsKey(bookingCode);
    }

    private Booking parseBooking(String line) {
        String[] parts = line.split("\\|");

        String bookingCode = parts[0];
        String showtimeId = parts[1];
        double totalPrice = Double.parseDouble(parts[2]); // parsed but ignored as booking.calculateTotalPrice() is more consistent

        Booking booking = new Booking(bookingCode, showtimeId);

        String[] itemParts = parts[3].split(";");

        for (String item : itemParts) {
            String[] fields = item.split(",");
            String seatId = fields[0];
            AttendeeType type = AttendeeType.valueOf(fields[1]);
            double price = Double.parseDouble(fields[2]);

            booking.addBookingItem(new BookingItem(seatId, type, price));
        }

        booking.calculateTotalPrice();
        return booking;
    }

    private void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Booking booking = parseBooking(line);
                bookings.put(booking.getBookingCode(), booking);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read movies file", e);
        }
    }

    private String formatBooking(Booking b) {
        StringBuilder sb = new StringBuilder();

        sb.append(b.getBookingCode()).append("|")
                .append(b.getShowtimeId()).append("|")
                .append(String.format("%.2f", b.getTotalPrice())).append("|");

        var items = b.getBookingItems();
        for (int i = 0; i < items.size(); i++) {
            BookingItem item = items.get(i);
            sb.append(item.getSeatId()).append(",")
                    .append(item.getAttendeeType()).append(",")
                    .append(String.format("%.2f", item.getItemPrice()));

            if (i < items.size() - 1) {
                sb.append(";");
            }
        }

        return sb.toString();
    }

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filepath))) {
            for (Booking b : bookings.values()) {
                pw.println(formatBooking(b));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save bookings file", e);
        }
    }
}
