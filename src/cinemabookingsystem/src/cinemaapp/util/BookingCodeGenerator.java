package cinemaapp.util;

import java.util.UUID;

public class BookingCodeGenerator {

    public String generateBookingCode() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
