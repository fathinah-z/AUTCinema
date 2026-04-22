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
    private final MovieRepository movieRepo;
    private final BookingCodeGenerator codeGenerator;
    private final PricingService pricingService;

    public MakeBookingService(BookingRepository bookingRepo,
            ScreenRepository screenRepo,
            ShowtimeRepository showtimeRepo,
            ShowSeatRepository showSeatRepo,
            MovieRepository movieRepo,
            BookingCodeGenerator codeGenerator,
            PricingService pricingService) {
        this.bookingRepo = bookingRepo;
        this.screenRepo = screenRepo;
        this.showtimeRepo = showtimeRepo;
        this.showSeatRepo = showSeatRepo;
        this.movieRepo = movieRepo;
        this.codeGenerator = codeGenerator;
        this.pricingService = pricingService;

        // Clean up abandoned reservations
        showSeatRepo.resetAllReservedSeats();
    }

    public Screen getScreenByShow(String showtimeId) {
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
        return getScreenByShow(showtimeId).getSeatingLayout().stream()
                .anyMatch(s -> s.getSeatId().equals(seatId));
    }

    public boolean isSeatAvailable(String showtimeId, String seatId) {
        SeatStatus status = getSeatStatusMap(showtimeId).get(seatId);
        return status == SeatStatus.AVAILABLE;
    }

    public void reserveSeat(String showtimeId, String seatId) {
        showSeatRepo.updateSeatStatus(showtimeId, seatId, SeatStatus.RESERVED);
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

    public String getWarningByRating(MovieRating rating) {
        switch (rating) {
            case R13:
                return "WARNING: This movie is rated R13.\nEntry is restricted to viewers aged 13 and above.";
            case R16:
                return "WARNING: This movie is rated R16.\nEntry is restricted to viewers aged 16 and above.";
            case R18:
                return "WARNING: This movie is rated R18.\nEntry is strictly limited to adults aged 18 and above.";
            default:
                return null; // no warning for G, PG, M
        }
    }
    
    public double calculateItemPrice(AttendeeType type, double basePrice) {
        return pricingService.calculateItemPrice(type, basePrice);
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

    public String makeBooking(String showtimeId, Map<String, AttendeeType> seatAttendeeMap) {
        Showtime showtime = showtimeRepo.findById(showtimeId);
        if (showtime == null) {
            return null;
        }

        Movie movie = movieRepo.findById(showtime.getMovieId());

        // Build booking
        String bookingCode = codeGenerator.generateUniqueCode(bookingRepo);
        Booking booking = new Booking(bookingCode, showtimeId);

        for (Map.Entry<String, AttendeeType> entry : seatAttendeeMap.entrySet()) {
            // child cannot book R18 movie
            if (movie.getRating() == MovieRating.R18 && entry.getValue() == AttendeeType.CHILD) {
                throw new IllegalArgumentException("Children cannot book R18 movies.");
            }

            String seatId = entry.getKey();
            AttendeeType attendeeType = entry.getValue();
            double itemPrice = pricingService.calculateItemPrice(attendeeType, showtime.getBasePrice());
            booking.addBookingItem(new BookingItem(seatId, attendeeType, itemPrice));
            showSeatRepo.updateSeatStatus(showtimeId, seatId, SeatStatus.BOOKED);
        }

        booking.calculateTotalPrice();
        bookingRepo.saveBooking(booking);
        return bookingCode;
    }
}
