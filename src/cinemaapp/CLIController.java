package cinemaapp;

import cinemaapp.dto.MovieDetails;
import cinemaapp.dto.ShowInfo;
import cinemaapp.filter.*;
import cinemaapp.model.*;
import cinemaapp.service.*;
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

    private static class BookingSummary {
        String movieTitle;
        String showtimeFormatted;
        Collection<String> seats;
        double totalPrice;
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
            System.out.print("\nSelect an option number: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    browse();
                    break;
                case "2":
                    cancelBooking();
                    break;
                case "3":
                    System.out.println("\nThank you for using AUT Cinema. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection. Please try again.");
            }
        }
    }

    public void browse() {
        while (true) {
            List<Movie> movies = browsingService.getMovies();
            if (movies.isEmpty()) {
                System.out.println("No movies currently available.");
                return;
            }

            printMovieList(movies);

            Movie selectedMovie = selectMovie(movies);
            if (selectedMovie == null) {
                return; // back to main menu
            }
            MovieDetails details = browsingService.getMovieDetails(selectedMovie.getMovieId());
            if (details == null) {
                System.out.println("Could not load movie details.");
                return;
            }

            printMovieDetails(details);

            List<ShowInfo> shows = details.getShowtimes();
            if (shows.isEmpty()) {
                System.out.println("No showtimes available.");
                continue;
            }

            printShowtimeList(shows);

            Showtime selectedShow = selectShowtime(shows);
            if (selectedShow == null) {
                continue; // back to movie list
            }
            if (confirmMakeBooking(details.getMovie(), selectedShow)) {
                makeBooking(
                        selectedShow.getShowtimeId(),
                        selectedShow.getBasePrice()
                );
                return;
            } else {
                System.out.println("Booking cancelled. Returning to main menu.");
            }
        }
    }

    public void makeBooking(String showtimeId, double basePrice) {
        Screen screen = makeBookingService.getScreen(showtimeId);
        Map<String, AttendeeType> cart = new LinkedHashMap<>();

        boolean bookingSeats = true;

        while (bookingSeats) {

            printSeatMap(showtimeId);

            boolean useFilters = askUseFilters();
            List<SeatFilter> filters = useFilters ? askFilters(screen) : new ArrayList<>();

            List<Seat> filteredSeats = makeBookingService.getFilteredSeats(showtimeId, filters);
            if (filteredSeats.isEmpty()) {
                System.out.println("No seats match your filters.");
                continue;
            }

            printFilteredSeats(filteredSeats);

            String seatId = selectSeatId(showtimeId, filteredSeats, useFilters);

            makeBookingService.reserveSeat(showtimeId, seatId);

            printAttendeeTypes();
            AttendeeType type = selectAttendeeType();

            if (!confirmSeatChoice(seatId, type)) {
                makeBookingService.unreserveSeat(showtimeId, seatId);
                continue;
            }

            cart.put(seatId, type);

            bookingSeats = askBookAnotherSeat();
        }

        printBookingSummary(buildSummaryFromCart(showtimeId, cart));

        if (!confirmBooking(showtimeId, cart)) {
            return; // user cancelled
        }

        processPayment(showtimeId, cart);
    }

    public void cancelBooking() {
        Booking booking = lookupBooking();

        if (booking == null) {
            System.out.println("No booking found with that code. Returning to main menu.");
            return;
        }

        printBookingSummary(buildSummaryFromBooking(booking));

        if (!confirmCancellation(booking)) {
            return;
        }

        performCancellation(booking);
    }

    //===HELPER METHODS===
    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String wrap(String text, int width) {
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

    private void printBookingSummary(BookingSummary summary) {
        System.out.println("\n====================================");
        System.out.println("        Booking Summary");
        System.out.println("====================================");

        System.out.println("Movie Title : " + summary.movieTitle);
        System.out.println("Showtime    : " + summary.showtimeFormatted);
        System.out.println("Seats       : " + summary.seats);
        System.out.printf("Total Cost  : $%.2f%n", summary.totalPrice);

        System.out.println("====================================");
    }

    //---Browse Methods--
    private void printMovieList(List<Movie> movies) {
        if (movies.isEmpty()) {
            System.out.println("No movies currently available.");
            return;
        }

        System.out.println("\n--- Now Showing ---");
        for (int i = 0; i < movies.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, movies.get(i));
        }
    }

    private Movie selectMovie(List<Movie> movies) {
        while (true) {
            System.out.print("\nSelect a movie (or X to go back): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("x")) {
                return null;
            }

            int idx = parseIntSafe(input) - 1;
            if (idx >= 0 && idx < movies.size()) {
                return movies.get(idx);
            }

            System.out.println("Invalid selection. Try again.");
        }
    }

    private void printMovieDetails(MovieDetails details) {
        Movie m = details.getMovie();

        System.out.println("\n--- " + m.getTitle() + " ---");
        System.out.println("Rating  : " + m.getRating());
        System.out.println("Runtime : " + m.getRuntime() + " min");
        System.out.println("Synopsis: " + wrap(m.getDescription(), 80));
        System.out.println("\nAvailable Showtimes:");

    }

    private void printShowtimeList(List<ShowInfo> shows) {
        for (int i = 0; i < shows.size(); i++) {
            ShowInfo si = shows.get(i);
            Showtime st = si.getShowtime();

            System.out.printf("  %d. %s  |  Available Seats: %d  |  Base Price: $%.2f%n",
                    i + 1,
                    st.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a")),
                    si.getAvailSeats(),
                    st.getBasePrice());
        }
    }

    private Showtime selectShowtime(List<ShowInfo> shows) {
        while (true) {
            System.out.print("\nSelect showtime number (or x to go back): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("x")) {
                return null;
            }

            int idx = parseIntSafe(input) - 1;
            if (idx >= 0 && idx < shows.size()) {
                ShowInfo show = shows.get(idx);
                return show.getShowtime();
            }

            System.out.println("Invalid selection. Try again.");
        }
    }

    private boolean confirmMakeBooking(Movie movie, Showtime show) {
        while (true) {
            System.out.printf(
                    "\nYou are about to make a booking for %s on %s. Confirm? (y/n): ",
                    movie.getTitle(),
                    show.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a"))
            );

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y")) {
                return true;
            }
            if (input.equals("n")) {
                return false;
            }

            System.out.println("Invalid input. Please enter y or n.");
        }
    }

    //---Make Booking Methods---
    private void printSeatMap(String showtimeId) {
        List<Seat> layout = makeBookingService.getScreen(showtimeId).getSeatingLayout();
        Map<String, SeatStatus> statusMap = makeBookingService.getSeatStatusMap(showtimeId);

        System.out.print("\nSeating Layout (row A is closest to the screen):");
        char currentRow = ' ';
        for (Seat seat : layout) {
            if (seat.getRow() != currentRow) {
                currentRow = seat.getRow();
                System.out.println(); // new row
                System.out.print(currentRow + "  ");
            }

            String id = seat.getSeatId();
            SeatStatus status = statusMap.get(id);

            String symbol = switch (status) {
                case BOOKED, RESERVED ->
                    "XXX";
                case AVAILABLE ->
                    id;
            };

            // if near aisle show with *
            if (seat.isNearAisle()) {
                symbol = symbol + "*";
            }

            // if accesible colour blue
            if (seat.isAccessible()) {
                symbol = "\u001B[34m" + symbol + "\u001B[0m";
            }

            System.out.printf("[%s]", symbol);
        }
        System.out.println("");

        System.out.println("\nLegend:");
        System.out.println("  XXX  = Booked/Reserved");
        System.out.println("  ID   = Available seat");
        System.out.println("  \u001B[34mID\u001B[0m   = Accessible seat");
        System.out.println("  ID*  = Aisle seat");
    }

    private boolean askUseFilters() {
        while (true) {
            System.out.print("\nUse filters? (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y")) {
                return true;
            }
            if (input.equals("n")) {
                return false;
            }

            System.out.println("Invalid input. Please enter y or n.");
        }
    }

    private List<SeatFilter> askFilters(Screen screen) {
        List<SeatFilter> filters = new ArrayList<>();

        // Aisle filter
        while (true) {
            System.out.print("\nFilter: aisle seats only? (y/n): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("y")) {
                filters.add(new AisleFilter(true));
                break;
            }
            if (input.equalsIgnoreCase("n")) {
                break;
            }

            System.out.println("Invalid input. Please enter y or n.");
        }

        // Accessible filter
        while (true) {
            System.out.print("\nFilter: accessible seats only? (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("y")) {
                filters.add(new AccessibleFilter(true));
                break;
            }
            if (input.equals("n")) {
                break;
            }

            System.out.println("Invalid input. Please enter y or n.");
        }

        // Row range filter
        while (true) {
            System.out.print("\nFilter by row range? (y/n): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("n")) {
                break;
            }
            if (input.equalsIgnoreCase("y")) {
                System.out.printf("\nIn this screen, the min row is %c and the max row is %c.",
                        screen.getFirstRow(), screen.getLastRow());

                System.out.printf("\nEnter minimum row %c-%c: ",
                        screen.getFirstRow(), screen.getLastRow());

                char minRow = askRow(screen.getFirstRow(), screen.getLastRow());

                System.out.printf("\nEnter maximum row %c-%c: ",
                        minRow, screen.getLastRow());

                char maxRow = askRow(minRow, screen.getLastRow());

                filters.add(new RowRangeFilter(minRow, maxRow));
                break;
            }

            System.out.println("Invalid input. Please enter y or n.");
        }

        return filters;
    }

    private char askRow(char minAllowed, char maxAllowed) {
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();

            if (input.length() == 1) {
                char row = input.charAt(0);

                if (row >= minAllowed && row <= maxAllowed) {
                    return row;
                }
            }

            System.out.printf("Invalid row. Please enter a letter between %c and %c: ",
                    minAllowed, maxAllowed);
        }
    }

    private void printFilteredSeats(List<Seat> seats) {
        System.out.println("\nFiltered Available Seats:");
        for (Seat s : seats) {
            System.out.printf("  %s (Row %c, Aisle:%b, Accessible:%b)%n",
                    s.getSeatId(),
                    s.getRow(),
                    s.isNearAisle(),
                    s.isAccessible());
        }
    }

    private String selectSeatId(String showtimeId, List<Seat> filteredSeats, boolean usedFilters) {
        while (true) {
            System.out.print("\nEnter Seat ID: ");
            String input = scanner.nextLine().trim().toUpperCase();

            if (!makeBookingService.seatExists(showtimeId, input)) {
                System.out.println("Invalid seat ID. Try again.");
                continue;
            }

            if (!makeBookingService.isSeatAvailable(showtimeId, input)) {
                System.out.println("Seat is already booked or reserved. Try another.");
                continue;
            }

            boolean inFiltered = filteredSeats.stream()
                    .anyMatch(s -> s.getSeatId().equals(input));

            if (!inFiltered && usedFilters) {
                System.out.printf("Seat %s is NOT in your filtered options. Proceed anyway? (y/n): ", input);
                while (true) {
                    String confirm = scanner.nextLine().trim();

                    if (confirm.equalsIgnoreCase("y")) {
                        return input; // user accepts seat out of filter
                    }
                    if (confirm.equalsIgnoreCase("n")) {
                        break; // exit confirmation loop
                    }

                    System.out.println("Invalid input. Please enter y or n.");
                }

                continue; // back to select seat
            }

            return input;
        }
    }

    private void printAttendeeTypes() {
        System.out.println("\nAttendee Types:");
        AttendeeType[] types = AttendeeType.values();

        for (int i = 0; i < types.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, types[i]);
        }
    }

    private AttendeeType selectAttendeeType() {
        AttendeeType[] types = AttendeeType.values();

        while (true) {
            System.out.print("\nChoose attendee type number: ");
            String input = scanner.nextLine().trim();

            int idx = parseIntSafe(input) - 1;

            if (idx >= 0 && idx < types.length) {
                return types[idx];
            }

            System.out.println("Invalid selection. Try again.");
        }
    }

    private boolean confirmSeatChoice(String seatId, AttendeeType type) {
        while (true) {
            System.out.printf("\nConfirm seat %s for (%s)? (y/n): ",
                    seatId, type);
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("n")) {
                return false;
            }

            System.out.println("Invalid input. Enter y or n.");
        }
    }

    private boolean askBookAnotherSeat() {
        while (true) {
            System.out.print("\nBook another seat? (y/n): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("n")) {
                return false;
            }

            System.out.println("Invalid input. Enter y or n.");
        }
    }

    private BookingSummary buildSummaryFromCart(String showtimeId, Map<String, AttendeeType> cart) {
        Showtime st = makeBookingService.getShowtimeById(showtimeId);
        Movie movie = browsingService.getMovieById(st.getMovieId());

        BookingSummary summary = new BookingSummary();
        summary.movieTitle = movie.getTitle();
        summary.showtimeFormatted = st.getDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a"));
        summary.seats = cart.keySet();
        summary.totalPrice = makeBookingService.calculateCartTotal(showtimeId, cart);

        return summary;
    }

    private boolean confirmBooking(String showtimeId, Map<String, AttendeeType> cart) {
        while (true) {
            System.out.print("\nConfirm booking? (y/n): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("y")) {
                return true;
            }

            if (input.equalsIgnoreCase("n")) {
                // Release all reserved seats
                for (String seatId : cart.keySet()) {
                    makeBookingService.unreserveSeat(showtimeId, seatId);
                }
                System.out.println("Booking cancelled. Returning to main menu.");
                return false;
            }

            System.out.println("Invalid input. Enter y or n.");
        }
    }

    private void processPayment(String showtimeId, Map<String, AttendeeType> cart) {
        while (true) {
            System.out.print("\nHave you made the payment? (y/n): ");
            String paid = scanner.nextLine().trim();

            if (paid.equalsIgnoreCase("y")) {
                String bookingCode = makeBookingService.makeBooking(showtimeId, cart);
                if (bookingCode == null) {
                    // Release reserved seats
                    for (String seatId : cart.keySet()) {
                        makeBookingService.unreserveSeat(showtimeId, seatId);
                    }
                    System.out.println("Booking failed. Returning to main menu.");
                    return;
                }
                System.out.println("\nBooking confirmed!");
                System.out.println("Your booking code is: " + bookingCode);
                return;
            }

            if (!paid.equalsIgnoreCase("n")) {
                System.out.println("Invalid input. Enter y or n.");
                continue;
            }

            // payment = no
            while (true) {
                System.out.print("\nWould you like to try again? (y/n): ");
                String retry = scanner.nextLine().trim();

                if (retry.equalsIgnoreCase("y")) {
                    break; // ask payment again
                }

                if (retry.equalsIgnoreCase("n")) {
                    // Release reserved seats
                    for (String seatId : cart.keySet()) {
                        makeBookingService.unreserveSeat(showtimeId, seatId);
                    }
                    System.out.println("Booking cancelled. Returning to main menu.");
                    return;
                }

                System.out.println("Invalid input. Enter y or n.");
            }
        }
    }

    //---Cancel Booking Methods---
    private Booking lookupBooking() {
        System.out.print("\nEnter your booking code to cancel: ");
        String code = scanner.nextLine().trim();

        return cancelBookingService.findBookingByCode(code);
    }

    private BookingSummary buildSummaryFromBooking(Booking booking) {
        Showtime st = cancelBookingService.getShowtimeById(booking.getShowtimeId());
        Movie movie = browsingService.getMovieById(st.getMovieId());

        BookingSummary summary = new BookingSummary();
        summary.movieTitle = movie.getTitle();
        summary.showtimeFormatted = st.getDateTime()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a"));

        // get seat ids from booking items
        List<String> seatIds = new ArrayList<>();
        for (BookingItem item : booking.getBookingItems()) {
            seatIds.add(item.getSeatId());
        }
        summary.seats = seatIds;

        summary.totalPrice = booking.getTotalPrice();

        return summary;
    }

    private boolean confirmCancellation(Booking booking) {
        boolean canRefund = refundEligible(booking);
        if (canRefund) {
            System.out.println("\nRefund is eligible as cancellation will be at least 5 days before showtime.");
        }
        else {
            System.out.println("\nRefund is not eligible as cancellation will be less than 5 days before booking.");
        }
        
        while (true) {
            System.out.print("\nConfirm cancellation? (y/n): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("n")) {
                System.out.println("Cancellation aborted. Returning to main menu.");
                return false;
            }

            System.out.println("Invalid input. Enter y or n.");
        }
    }

    private boolean refundEligible(Booking booking) {
        return cancelBookingService.isRefundEligible(booking, LocalDateTime.now());
    }
    
    private void performCancellation(Booking booking) {
        boolean canRefund = refundEligible(booking);
        boolean success = cancelBookingService.cancelBooking(booking);

        if (success) {
            System.out.println("Booking cancelled successfully.");
            if (canRefund) {
                System.out.println("Booking refunded.");
            }
        } else {
            System.out.println("Cancellation failed. Returning to main menu.");
        }
    }
    
}
