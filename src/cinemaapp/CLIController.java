package cinemaapp;

import cinemaapp.dto.MovieDetails;
import cinemaapp.dto.ShowInfo;
import cinemaapp.filter.*;
import cinemaapp.model.*;
import cinemaapp.repository.*;
import cinemaapp.service.*;
import cinemaapp.util.BookingCodeGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CLIController {

    private final BrowsingService browsingService;
    private final MakeBookingService makeBookingService;
    private final CancelBookingService cancelBookingService;

    private final Scanner scanner = new Scanner(System.in);

    public CLIController(BrowsingService browsingService,
            MakeBookingService makeBookingService,
            CancelBookingService cancelBookingService) {
        this.browsingService = browsingService;
        this.makeBookingService = makeBookingService;
        this.cancelBookingService = cancelBookingService;
    }

    public void start() {
        System.out.println("===========================================");
        System.out.println("       Welcome to AUT Cinema Booking       ");
        System.out.println("===========================================");

        boolean running = true;
        while (running) {
            System.out.println("\nMain Menu:");
            System.out.println("  1. Browse & Book");
            System.out.println("  2. Cancel a Booking");
            System.out.println("  3. Exit");
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    browse();
                    break;
                case "2":
                    cancelBooking();
                    break;
                case "3":
                    System.out.println("Thank you for using AUT Cinema. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please 1, 2, or 3.");
            }
        }
    }

    public void browse() {
        List<Movie> movies = browsingService.getMovies();
        if (movies.isEmpty()) {
            System.out.println("No movies currently available.");
            return;
        }

        System.out.println("\n--- Now Showing ---");
        for (int i = 0; i < movies.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, movies.get(i));
        }

        // make invalid
        System.out.print("\nEnter movie number for details (or 0 to go back): ");
        int idx = parseIntSafe(scanner.nextLine().trim()) - 1;
        if (idx < 0 || idx >= movies.size()) {
            return;
        }

        Movie selected = movies.get(idx);
        MovieDetails details = browsingService.getMovieDetails(selected.getMovieId());
        if (details == null) {
            System.out.println("Could not load movie details.");
            return;
        }

        System.out.println("\n--- " + details.getMovie().getTitle() + " ---");
        System.out.println("Rating  : " + details.getMovie().getRating());
        System.out.println("Runtime : " + details.getMovie().getRuntime() + " min");
        System.out.println("Synopsis: " + wrap(details.getMovie().getDescription(), 80));
        System.out.println("\nAvailable Showtimes:");
       
        List<ShowInfo> shows = details.getShowtimes();

        for (int i = 0; i < details.getShowtimes().size(); i++) {
            ShowInfo si = details.getShowtimes().get(i);

            System.out.printf("  %d. %s  |  Available Seats: %d  |  Base Price: $%.2f%n",
                    i + 1,
                    si.getShowtime().getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")),
                    si.getAvailSeats(),
                    si.getShowtime().getBasePrice());
        }

        // make invalid and merge booking and select into one
        System.out.print("\nSelect showtime (or 0 to go back): ");
        int showIdx = parseIntSafe(scanner.nextLine()) - 1;
        if (showIdx < 0 || showIdx >= shows.size()) {
            return;
        }

        ShowInfo selectedShow = shows.get(showIdx);
        String showtimeId = selectedShow.getShowtime().getShowtimeId();
        double basePrice = selectedShow.getShowtime().getBasePrice();

        System.out.print("Do you want to book a seat? (y/n): ");
        if ("y".equalsIgnoreCase(scanner.nextLine())) {
            makeBooking(showtimeId, basePrice);
        }

    }

    public void makeBooking(String showtimeId, double basePrice) {

        List<SeatFilter> filters = new ArrayList<>();

        System.out.print("Filter: aisle seats only? (y/n): ");
        if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
            filters.add(new AisleFilter(true));
        }
        System.out.print("Filter: accessible seats only? (y/n): ");
        if ("y".equalsIgnoreCase(scanner.nextLine().trim())) {
            filters.add(new AccessibleFilter(true));
        }

        // add row filter
        Map<String, AttendeeType> cart = new LinkedHashMap<>();
        boolean bookingSeats = true;

        while (bookingSeats) {

            List<Seat> availSeats = makeBookingService.getAvailSeats(showtimeId, filters);

            if (availSeats.isEmpty()) {
                System.out.println("No seats available.");
                return;
            }

            System.out.println("\nAvailable Seats:");
            for (Seat seat : availSeats) {
                System.out.printf("  %s Row:%c Aisle:%b Accessible:%b%n",
                        seat.getSeatId(), seat.getRow(),
                        seat.isNearAisle(), seat.isAccessible());
            }

            // make invalid
            System.out.print("\nEnter Seat ID: ");
            String seatId = scanner.nextLine().trim().toUpperCase();

            AttendeeType type = null;

            // make list
            while (type == null) {
                System.out.print("Attendee type (ADULT/CHILD/STUDENT/SENIOR): ");
                String input = scanner.nextLine().trim().toUpperCase();

                try {
                    type = AttendeeType.valueOf(input);
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid type. Please enter: ADULT, CHILD, STUDENT, or SENIOR.");
                }
            }

            cart.put(seatId, type);

            // make invalid
            System.out.print("Book another seat? (y/n): ");
            bookingSeats = "y".equalsIgnoreCase(scanner.nextLine());
        }

        while (true) {
            double total = 0.0;
            for (AttendeeType type : cart.values()) {
                total += basePrice * type.getPriceModifier();
            }

            System.out.printf("\nTotal cost: $%.2f%n", total);
            
            // make invalid
            System.out.println("Processing to payment...");
            System.out.print("Have you made the payment? (y/n): ");
            
            if ("y".equalsIgnoreCase(scanner.nextLine())) {
                makeBookingService.makeBooking(showtimeId, cart);
                System.out.println("Booking confirmed!");
                return;
            }
            
            //make invalid
            System.out.print("Would you like to try again? (y/n): ");
            if ("y".equalsIgnoreCase(scanner.nextLine())) {
                continue;
            }

            //makeinvalid
            System.out.print("Cancel booking? (y/n): ");
            if ("y".equalsIgnoreCase(scanner.nextLine())) {
                System.out.println("Booking cancelled. Returning to main menu.");
                return;
            }
        }
    }

    public void cancelBooking() {
        System.out.print("\nEnter your booking code to cancel: ");
        String code = scanner.nextLine().trim();

        // We need direct repo access here to look up the booking — injected via service layer in production
        // For this CLI, we re-use MakeBookingService's repo indirectly via CancelBookingService
        System.out.println("(Cancellation lookup is handled internally.)");
        System.out.println("Please contact support with code: " + code + " if this is a production system.");
        // In a full implementation, the CLIController would call cancelBookingService.cancelBooking(booking)
        // after fetching the booking from a BookingRepository reference here.
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String wrap(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int lineLength = 0;

        for (String word : text.split(" ")) {
            if (lineLength + word.length() > width) {
                sb.append("\n");
                lineLength = 0;
            }
            sb.append(word).append(" ");
            lineLength += word.length() + 1;
        }

        return sb.toString();
    }
}