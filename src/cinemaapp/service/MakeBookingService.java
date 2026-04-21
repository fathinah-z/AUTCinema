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

        // Clean up abandoned reservations
        showSeatRepo.resetAllReservedSeats();
    }

    public Screen getScreen(String showtimeId) {
        Showtime st = showtimeRepo.findById(showtimeId);
        Screen screen = screenRepo.findById(st.getScreenId());
        return screen;
    }

    public Showtime getShowtimeById(String showtimeId) {
        return showtimeRepo.findById(showtimeId);
    }

    public Map<String, SeatStatus> getSeatStatusMap(String showtimeId) {
        return showSeatRepo.findByShowtimeId(showtimeId);
    }

    public boolean seatExists(String showtimeId, String seatId) {
        return getScreen(showtimeId).getSeatingLayout().stream()
                .anyMatch(s -> s.getSeatId().equals(seatId));
    }

    public boolean isSeatAvailable(String showtimeId, String seatId) {
        SeatStatus status = getSeatStatusMap(showtimeId).get(seatId);
        return status == SeatStatus.AVAILABLE;
    }

    public boolean reserveSeat(String showtimeId, String seatId) {
        return showSeatRepo.tryReserveSeat(showtimeId, seatId);
    }

    public void unreserveSeat(String showtimeId, String seatId) {
        showSeatRepo.updateSeatStatus(showtimeId, seatId, SeatStatus.AVAILABLE);
    }

    public List<Seat> getFilteredSeats(String showtimeId, List<SeatFilter> filters) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        if (showtime == null) {
            return Collections.emptyList();
        }

        Screen screen = screenRepo.findById(showtime.getScreenId());
        if (screen == null) {
            return Collections.emptyList();
        }

        Map<String, SeatStatus> seatStatusMap = getSeatStatusMap(showtimeId);

        List<Seat> availableSeats = new ArrayList<>();
        for (Seat seat : screen.getSeatingLayout()) {
            SeatStatus status = seatStatusMap.getOrDefault(seat.getSeatId(), SeatStatus.AVAILABLE);
            if (status != SeatStatus.AVAILABLE) {
                continue;
            }

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

    public double calculateCartTotal(String showtimeId, Map<String, AttendeeType> cart) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        double basePrice = showtime.getBasePrice();

        double total = 0.0;
        for (AttendeeType type : cart.values()) {
            total += pricingService.calculateItemPrice(type, basePrice);
        }

        return total;
    }

    /**
     * Creates a booking for the given seat-attendee pairs under one showtime.
     * Returns true if booking succeeded, false otherwise.
     */
    public String makeBooking(String showtimeId, Map<String, AttendeeType> seatAttendeeMap) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        if (showtime == null) {
            return null;
        }

        // Build booking
        String bookingCode = codeGenerator.generateUniqueCode(bookingRepo);
        Booking booking = new Booking(bookingCode, showtimeId);

        for (Map.Entry<String, AttendeeType> entry : seatAttendeeMap.entrySet()) {
            String seatId = entry.getKey();
            AttendeeType attendeeType = entry.getValue();
            double itemPrice = pricingService.calculateItemPrice(attendeeType, showtime.getBasePrice());
            booking.addBookingItem(new BookingItem(seatId, attendeeType, itemPrice));
            if (!showSeatRepo.tryBookSeat(showtimeId, seatId)) {
                return null; // someone else booked it first
            }
        }

        booking.calculateTotalPrice();
        bookingRepo.saveBooking(booking);
        return bookingCode;
    }
}
