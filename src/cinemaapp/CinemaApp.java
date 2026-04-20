package cinemaapp;

import cinemaapp.repository.*;
import cinemaapp.service.*;
import cinemaapp.util.BookingCodeGenerator;

public class CinemaApp {

    private final CLIController cliControl;

    public CinemaApp() {
        // --- Repositories (File-based implementations) ---
        MovieRepository movieRepo = new FileMovieRepository();
        ShowtimeRepository showtimeRepo = new FileShowtimeRepository();
        ShowSeatRepository showSeatRepo = new FileShowSeatRepository();
        BookingRepository bookingRepo = new FileBookingRepository();
        ScreenRepository screenRepo = new FileScreenRepository();

        // --- Utilities ---
        BookingCodeGenerator codeGenerator = new BookingCodeGenerator();

        // --- Services ---
        PricingService pricingService = new PricingService();

        BrowsingService browsingService = new BrowsingService(
                movieRepo, showtimeRepo, showSeatRepo);

        MakeBookingService makeBookingService = new MakeBookingService(
                bookingRepo, screenRepo, showtimeRepo, showSeatRepo,
                codeGenerator, pricingService);

        CancelBookingService cancelBookingService = new CancelBookingService(
                bookingRepo, showSeatRepo);

        // --- Controller ---
        this.cliControl = new CLIController(
                browsingService, makeBookingService, cancelBookingService);
    }

    public static void main(String[] args) {
        new CinemaApp().cliControl.start();
    }
}
