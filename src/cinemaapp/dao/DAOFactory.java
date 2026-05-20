package cinemaapp.dao;

import cinemaapp.dao.derby.DerbyAccountDAO;
import cinemaapp.dao.derby.DerbyBookingDAO;
import cinemaapp.dao.derby.DerbyMovieDAO;
import cinemaapp.dao.derby.DerbyScreenDAO;
import cinemaapp.dao.derby.DerbyShowSeatDAO;
import cinemaapp.dao.derby.DerbyShowtimeDAO;
import cinemaapp.db.DatabaseManager;

/**
 * Abstract Factory that produces all DAO instances.
 *
 * Using a factory here achieves two goals:
 * <ol>
 *   <li>The GUI / service layer asks for a DAO by interface type and never
 *       depends on concrete Derby classes.</li>
 *   <li>Swapping from Derby to another database (or a mock for testing)
 *       requires changing only this class.</li>
 * </ol>
 *
 * This is a concrete implementation of the Abstract Factory pattern – a future
 * {@code MockDAOFactory} extending this class could return in-memory stubs for
 * unit tests without touching any other code.
 */
public class DAOFactory {

    private final DatabaseManager dbManager;

    // Lazy-initialised singletons so each DAO is created at most once
    private MovieDAO    movieDAO;
    private ScreenDAO   screenDAO;
    private ShowtimeDAO showtimeDAO;
    private ShowSeatDAO showSeatDAO;
    private BookingDAO  bookingDAO;
    private AccountDAO  accountDAO;

    public DAOFactory(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public MovieDAO getMovieDAO() {
        if (movieDAO == null) {
            movieDAO = new DerbyMovieDAO(dbManager);
        }
        return movieDAO;
    }

    public ScreenDAO getScreenDAO() {
        if (screenDAO == null) {
            screenDAO = new DerbyScreenDAO(dbManager);
        }
        return screenDAO;
    }

    public ShowtimeDAO getShowtimeDAO() {
        if (showtimeDAO == null) {
            showtimeDAO = new DerbyShowtimeDAO(dbManager);
        }
        return showtimeDAO;
    }

    public ShowSeatDAO getShowSeatDAO() {
        if (showSeatDAO == null) {
            showSeatDAO = new DerbyShowSeatDAO(dbManager);
        }
        return showSeatDAO;
    }

    public BookingDAO getBookingDAO() {
        if (bookingDAO == null) {
            bookingDAO = new DerbyBookingDAO(dbManager);
        }
        return bookingDAO;
    }

    public AccountDAO getAccountDAO() {
        if (accountDAO == null) {
            accountDAO = new DerbyAccountDAO(dbManager);
        }
        return accountDAO;
    }
}
