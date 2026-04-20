package cinemaapp.util;

import cinemaapp.repository.BookingRepository;

import java.util.UUID;

public class BookingCodeGenerator {

    public String generateUniqueCode(BookingRepository bookingRepo) {
        String code;

        do {
            code = generateBookingCode();
        } while (bookingRepo.existsByBookingCode(code));

        return code;
    }

    private String generateBookingCode() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
