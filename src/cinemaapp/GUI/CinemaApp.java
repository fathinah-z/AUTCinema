package cinemaapp.GUI;

import cinemaapp.dao.DAOFactory;
import cinemaapp.db.DatabaseInitialiser;
import cinemaapp.db.DatabaseManager;
import cinemaapp.GUI.MainFrame;
import cinemaapp.repository.*;
import cinemaapp.service.*;
import cinemaapp.util.BookingCodeGenerator;

import javax.swing.*;

/**
 * Application entry point for Project 2.
 *
 * This replaces the old CLI-based CinemaApp from Project 1.
 * CLIController.java can stay in the project — it is simply no longer called.
 *
 * Design patterns used here:
 *   Singleton        : DatabaseManager (one DB connection for the whole app)
 *   Abstract Factory : DAOFactory (produces all DAO instances)
 *   Adapter          : Db*Repository classes bridge P1 service interfaces to new DAOs
 *   MVC              : services = Model, gui package = View + Controller
 */
public class CinemaApp {

    public static void main(String[] args) {

        // 1. Initialise Derby (Singleton — creates CinemaBookingDB/ folder on first run)
        DatabaseManager dbManager = DatabaseManager.getInstance();
        try {
            new DatabaseInitialiser(dbManager).initialise();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to initialise database:\n" + e.getMessage(),
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Build all DAOs (Abstract Factory)
        DAOFactory factory = new DAOFactory(dbManager);

        // 3. Build DB-backed repositories (Adapter — wraps DAOs to fit P1 interfaces)
        DbBookingRepository  bookingRepo  = new DbBookingRepository(factory.getBookingDAO());
        DbMovieRepository    movieRepo    = new DbMovieRepository(factory.getMovieDAO());
        DbScreenRepository   screenRepo   = new DbScreenRepository(factory.getScreenDAO());
        DbShowtimeRepository showtimeRepo = new DbShowtimeRepository(factory.getShowtimeDAO());
        DbShowSeatRepository showSeatRepo = new DbShowSeatRepository(factory.getShowSeatDAO());

        // 4. Build services — identical to Project 1, zero changes needed
        PricingService       pricing    = new PricingService();
        BookingCodeGenerator codeGen    = new BookingCodeGenerator();
        BrowsingService      browsing   = new BrowsingService(movieRepo, showtimeRepo, showSeatRepo);
        MakeBookingService   makeBook   = new MakeBookingService(
                bookingRepo, screenRepo, showtimeRepo, showSeatRepo, movieRepo, codeGen, pricing);
        CancelBookingService cancel     = new CancelBookingService(bookingRepo, showSeatRepo, showtimeRepo);
        

        // 5. Shut Derby down cleanly when the window closes
        Runtime.getRuntime().addShutdownHook(new Thread(dbManager::shutdown));

        // 6. Launch GUI on Swing's event dispatch thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(
                    factory, bookingRepo, browsing, makeBook, cancel);
            frame.setVisible(true);
        });
    }
}