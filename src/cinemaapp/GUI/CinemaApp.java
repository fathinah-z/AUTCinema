package cinemaapp.GUI;

import cinemaapp.dao.DAOFactory;
import cinemaapp.db.DatabaseInitialiser;
import cinemaapp.db.DatabaseManager;
import cinemaapp.GUI.MainFrame;
import cinemaapp.repository.*;
import cinemaapp.service.*;
import cinemaapp.util.BookingCodeGenerator;

import javax.swing.*;

public class CinemaApp {

    public static void main(String[] args) {

        //Initialise Derby 
        DatabaseManager dbManager = DatabaseManager.getInstance();
        try {
            new DatabaseInitialiser(dbManager).initialise();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to initialise database:\n" + e.getMessage(),
                "Startup Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Build all DAOs 
        DAOFactory factory = new DAOFactory(dbManager);

        // Build DB-backed repositories 
        DbBookingRepository  bookingRepo  = new DbBookingRepository(factory.getBookingDAO());
        DbMovieRepository    movieRepo    = new DbMovieRepository(factory.getMovieDAO());
        DbScreenRepository   screenRepo   = new DbScreenRepository(factory.getScreenDAO());
        DbShowtimeRepository showtimeRepo = new DbShowtimeRepository(factory.getShowtimeDAO());
        DbShowSeatRepository showSeatRepo = new DbShowSeatRepository(factory.getShowSeatDAO());

        // Build services 
        PricingService       pricing    = new PricingService();
        BookingCodeGenerator codeGen    = new BookingCodeGenerator();
        BrowsingService      browsing   = new BrowsingService(movieRepo, showtimeRepo, showSeatRepo);
        MakeBookingService   makeBook   = new MakeBookingService(
                bookingRepo, screenRepo, showtimeRepo, showSeatRepo, movieRepo, codeGen, pricing);
        CancelBookingService cancel     = new CancelBookingService(bookingRepo, showSeatRepo, showtimeRepo);
        

        // Shut Derby down cleanly when the window closes
        Runtime.getRuntime().addShutdownHook(new Thread(dbManager::shutdown));

        // Launch GUI on Swing's event dispatch thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(
                    factory, bookingRepo, browsing, makeBook, cancel);
            frame.setVisible(true);
        });
    }
}