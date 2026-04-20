package cinemaapp.service;

import cinemaapp.filter.SeatFilter;
import cinemaapp.model.*;
import cinemaapp.repository.*;
import cinemaapp.util.BookingCodeGenerator;

import java.util.*;

public class MakeBookingService {

    private final BookingRepository bookingRepo;
    private final ScreenRepository screenRepo;
    private final ShowtimeRepository showtimeRepo;
    private final ShowSeatRepository showSeatRepo;
    private final BookingCodeGenerator codeGenerator;
    private final PricingService pricingService;

    public MakeBookingService(BookingRepository bookingRepo,
                              ScreenRepository screenRepo,
                              ShowtimeRepository showtimeRepo,
                              ShowSeatRepository showSeatRepo,
                              BookingCodeGenerator codeGenerator,
                              PricingService pricingService) {
        this.bookingRepo = bookingRepo;
        this.screenRepo = screenRepo;
        this.showtimeRepo = showtimeRepo;
        this.showSeatRepo = showSeatRepo;
        this.codeGenerator = codeGenerator;
        this.pricingService = pricingService;
    }

    /**
     * Returns all available seats for a showtime that pass the given filters.
     */
    public List<Seat> getAvailSeats(String showtimeId, List<SeatFilter> filters) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        if (showtime == null) return Collections.emptyList();

        Screen screen = screenRepo.findById(showtime.getScreenId());
        if (screen == null) return Collections.emptyList();

        HashMap<String, SeatStatus> seatStatusMap = showSeatRepo.findByShowtimeId(showtimeId);

        List<Seat> availableSeats = new ArrayList<>();
        for (Seat seat : screen.getSeatingLayout()) {
            SeatStatus status = seatStatusMap.getOrDefault(seat.getSeatId(), SeatStatus.AVAILABLE);
            if (status != SeatStatus.AVAILABLE) continue;

            boolean passesAllFilters = true;
            if (filters != null) {
                for (SeatFilter filter : filters) {
                    if (!filter.matches(seat)) {
                        passesAllFilters = false;
                        break;
                    }
                }
            }
            if (passesAllFilters) {
                availableSeats.add(seat);
            }
        }
        return availableSeats;
    }

    /**
     * Creates a booking for the given seat-attendee pairs under one showtime.
     * Returns true if booking succeeded, false otherwise.
     */
    public boolean makeBooking(String showtimeId, Map<String, AttendeeType> seatAttendeeMap) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        if (showtime == null) return false;

        // Verify all seats are still available
        HashMap<String, SeatStatus> currentStatuses = showSeatRepo.findByShowtimeId(showtimeId);
        for (String seatId : seatAttendeeMap.keySet()) {
            SeatStatus status = currentStatuses.getOrDefault(seatId, SeatStatus.AVAILABLE);
            if (status != SeatStatus.AVAILABLE) {
                System.out.println("Seat " + seatId + " is no longer available.");
                return false;
            }
        }

        // Build booking
        String bookingCode = codeGenerator.generateUniqueCode(bookingRepo);
        Booking booking = new Booking(bookingCode, showtimeId);

        for (Map.Entry<String, AttendeeType> entry : seatAttendeeMap.entrySet()) {
            String seatId = entry.getKey();
            AttendeeType attendeeType = entry.getValue();
            double itemPrice = pricingService.calculateItemPrice(attendeeType, showtime.getBasePrice());
            booking.addBookingItem(new BookingItem(seatId, attendeeType, itemPrice));
            showSeatRepo.updateSeatStatus(showtimeId, seatId, SeatStatus.BOOKED);
        }

        booking.calculateTotalPrice();
        bookingRepo.saveBooking(booking);
        System.out.println("Booking confirmed! Code: " + bookingCode);
        return true;
    }
}
