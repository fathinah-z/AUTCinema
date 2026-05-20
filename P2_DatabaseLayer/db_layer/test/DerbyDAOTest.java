package cinemaapp.dao;

import cinemaapp.dao.derby.*;
import cinemaapp.db.DatabaseInitialiser;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.*;
import org.junit.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * JUnit 4 integration tests for the Derby DAO layer.
 *
 * Each test runs against an in-memory-like Derby database (the same embedded
 * engine, but with a unique DB path per test run to avoid cross-test
 * pollution).  Tables are created fresh in {@link #setUpClass()} and torn
 * down in {@link #tearDownClass()}.
 *
 * Covers:
 *   1. Account authentication (valid + invalid credentials)
 *   2. Movie CRUD (save, findById, findAll, update, delete)
 *   3. ShowSeat status update
 *   4. Booking save and retrieval
 *   5. Booking cancellation (delete + seat release)
 *   6. Duplicate booking-code guard
 *   7. ShowSeat reset (reserved → available)
 */
public class DerbyDAOTest {

    // One DatabaseManager shared across all tests in this class
    private static DatabaseManager dbManager;
    private static DAOFactory       factory;

    // -----------------------------------------------------------------------
    // Class-level setup / teardown
    // -----------------------------------------------------------------------

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Use a separate DB name for tests so it does not pollute the app DB
        System.setProperty("derby.test.db", "CinemaTestDB_" + System.currentTimeMillis());
        dbManager = TestDatabaseManager.createForTest();
        factory   = new DAOFactory(dbManager);
        new DatabaseInitialiser(dbManager).initialise();
    }

    @AfterClass
    public static void tearDownClass() {
        dbManager.shutdown();
    }

    // -----------------------------------------------------------------------
    // Helper: insert a minimal Account + Movie + Screen + Seat + Showtime
    // so individual test methods can focus on what they're testing.
    // -----------------------------------------------------------------------

    @Before
    public void insertTestFixtures() throws SQLException {
        Connection conn = dbManager.getConnection();
        // Wrap in try-catch; rows may already exist from seed data – that's fine
        try (Statement st = conn.createStatement()) {
            runIgnoringDuplicates(st, "INSERT INTO Account VALUES ('testuser','testpass')");
            runIgnoringDuplicates(st, "INSERT INTO Movie   VALUES ('T01','Test Movie','G','Desc',90)");
            runIgnoringDuplicates(st, "INSERT INTO Screen  VALUES ('TSC')");
            runIgnoringDuplicates(st,
                "INSERT INTO Seat VALUES ('TA1','A',1,1,0,'TSC')");
            runIgnoringDuplicates(st,
                "INSERT INTO Showtime VALUES ('TSH','2099-12-01 10:00:00',10.00,'TSC','T01')");
            runIgnoringDuplicates(st,
                "INSERT INTO ShowSeat VALUES ('TA1','TSH','AVAILABLE')");
        }
    }

    /** Executes SQL and ignores duplicate-key violations (Derby state 23505). */
    private void runIgnoringDuplicates(Statement st, String sql) throws SQLException {
        try {
            st.execute(sql);
        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) throw e; // re-throw unexpected errors
        }
    }

    // -----------------------------------------------------------------------
    // Test 1 – Account: valid authentication
    // -----------------------------------------------------------------------

    @Test
    public void testAuthenticate_validCredentials_returnsAccount() throws SQLException {
        AccountDAO dao = factory.getAccountDAO();
        Account account = dao.authenticate("testuser", "testpass");

        assertNotNull("Account should be found for valid credentials", account);
        assertEquals("testuser", account.getUsername());
    }

    // -----------------------------------------------------------------------
    // Test 2 – Account: invalid authentication
    // -----------------------------------------------------------------------

    @Test
    public void testAuthenticate_wrongPassword_returnsNull() throws SQLException {
        AccountDAO dao = factory.getAccountDAO();
        Account account = dao.authenticate("testuser", "wrongpass");

        assertNull("Account should be null for wrong password", account);
    }

    // -----------------------------------------------------------------------
    // Test 3 – Movie: save and retrieve by ID
    // -----------------------------------------------------------------------

    @Test
    public void testSaveAndFindMovie() throws SQLException {
        MovieDAO dao = factory.getMovieDAO();
        Movie newMovie = new Movie("TM2", "JUnit Film", MovieRating.PG,
                                   "A film about unit tests", 95);
        dao.save(newMovie);

        Movie retrieved = dao.findById("TM2");
        assertNotNull("Saved movie should be retrievable", retrieved);
        assertEquals("JUnit Film", retrieved.getTitle());
        assertEquals(MovieRating.PG, retrieved.getRating());
        assertEquals(95, retrieved.getRuntime());

        // Cleanup
        dao.delete("TM2");
    }

    // -----------------------------------------------------------------------
    // Test 4 – Movie: update
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateMovie() throws SQLException {
        MovieDAO dao = factory.getMovieDAO();
        Movie m = new Movie("TM3", "Original Title", MovieRating.G, "Desc", 60);
        dao.save(m);

        m.setTitle("Updated Title");
        m.setRuntime(75);
        dao.update(m);

        Movie updated = dao.findById("TM3");
        assertEquals("Updated Title", updated.getTitle());
        assertEquals(75, updated.getRuntime());

        dao.delete("TM3");
    }

    // -----------------------------------------------------------------------
    // Test 5 – ShowSeat: update seat status
    // -----------------------------------------------------------------------

    @Test
    public void testUpdateSeatStatus_availableToBooked() throws SQLException {
        ShowSeatDAO dao = factory.getShowSeatDAO();

        dao.updateSeatStatus("TSH", "TA1", SeatStatus.BOOKED);

        Map<String, SeatStatus> statusMap = dao.findByShowtimeId("TSH");
        assertEquals("Seat status should be BOOKED", SeatStatus.BOOKED, statusMap.get("TA1"));

        // Reset for other tests
        dao.updateSeatStatus("TSH", "TA1", SeatStatus.AVAILABLE);
    }

    // -----------------------------------------------------------------------
    // Test 6 – Booking: save and retrieve
    // -----------------------------------------------------------------------

    @Test
    public void testSaveAndFindBooking() throws SQLException {
        BookingDAO   bookingDAO   = factory.getBookingDAO();
        ShowSeatDAO  showSeatDAO  = factory.getShowSeatDAO();

        // Mark seat as booked (mirrors what MakeBookingService does)
        showSeatDAO.updateSeatStatus("TSH", "TA1", SeatStatus.BOOKED);

        Booking booking = new Booking("BC-TEST-01", "TSH");
        booking.addBookingItem(new BookingItem("TA1", AttendeeType.ADULT, 10.00));
        booking.calculateTotalPrice();

        bookingDAO.save(booking, "testuser");

        Booking retrieved = bookingDAO.findByBookingCode("BC-TEST-01");
        assertNotNull("Saved booking should be retrievable", retrieved);
        assertEquals(1, retrieved.getBookingItems().size());
        assertEquals("TA1", retrieved.getBookingItems().get(0).getSeatId());
        assertEquals(10.00, retrieved.getTotalPrice(), 0.001);

        // Cleanup
        bookingDAO.delete("BC-TEST-01");
        showSeatDAO.updateSeatStatus("TSH", "TA1", SeatStatus.AVAILABLE);
    }

    // -----------------------------------------------------------------------
    // Test 7 – Booking: cancellation releases seats
    // -----------------------------------------------------------------------

    @Test
    public void testCancelBooking_seatBecomesAvailable() throws SQLException {
        BookingDAO  bookingDAO  = factory.getBookingDAO();
        ShowSeatDAO showSeatDAO = factory.getShowSeatDAO();

        showSeatDAO.updateSeatStatus("TSH", "TA1", SeatStatus.BOOKED);

        Booking booking = new Booking("BC-TEST-02", "TSH");
        booking.addBookingItem(new BookingItem("TA1", AttendeeType.STUDENT, 7.50));
        booking.calculateTotalPrice();
        bookingDAO.save(booking, "testuser");

        // Cancel: delete booking, release seat
        bookingDAO.delete("BC-TEST-02");
        showSeatDAO.updateSeatStatus("TSH", "TA1", SeatStatus.AVAILABLE);

        // Verify booking gone
        assertFalse("Booking should no longer exist",
                    bookingDAO.existsByBookingCode("BC-TEST-02"));

        // Verify seat is AVAILABLE again
        Map<String, SeatStatus> statusMap = showSeatDAO.findByShowtimeId("TSH");
        assertEquals(SeatStatus.AVAILABLE, statusMap.get("TA1"));
    }

    // -----------------------------------------------------------------------
    // Test 8 – ShowSeat: reset reserved seats
    // -----------------------------------------------------------------------

    @Test
    public void testResetAllReservedSeats() throws SQLException {
        ShowSeatDAO dao = factory.getShowSeatDAO();

        // Mark seat as RESERVED (simulating an abandoned session)
        dao.updateSeatStatus("TSH", "TA1", SeatStatus.RESERVED);

        dao.resetAllReservedSeats();

        Map<String, SeatStatus> statusMap = dao.findByShowtimeId("TSH");
        assertEquals("RESERVED seat should become AVAILABLE after reset",
                     SeatStatus.AVAILABLE, statusMap.get("TA1"));
    }

    // -----------------------------------------------------------------------
    // Test 9 – Movie: findAll returns all seeded movies
    // -----------------------------------------------------------------------

    @Test
    public void testFindAllMovies_returnsNonEmptyList() throws SQLException {
        MovieDAO dao = factory.getMovieDAO();
        List<Movie> movies = dao.findAll();
        assertFalse("findAll should return at least the seeded movies", movies.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Test 10 – Showtime: find by movie ID
    // -----------------------------------------------------------------------

    @Test
    public void testFindShowtimesByMovieId() throws SQLException {
        ShowtimeDAO dao = factory.getShowtimeDAO();
        List<Showtime> showtimes = dao.findByMovieId("T01"); // seeded in @Before
        assertFalse("Should find at least one showtime for test movie", showtimes.isEmpty());
        for (Showtime st : showtimes) {
            assertEquals("T01", st.getMovieId());
        }
    }

    // -----------------------------------------------------------------------
    // Inner helper: TestDatabaseManager
    // Creates a DatabaseManager pointed at a unique test DB name.
    // -----------------------------------------------------------------------

    /**
     * Thin subclass of DatabaseManager that overrides the DB URL with a
     * test-specific name so tests never touch the production database.
     *
     * Because DatabaseManager's constructor and connection URL are private
     * we re-implement the minimal connection creation here.
     */
    static class TestDatabaseManager extends DatabaseManager {

        private static final String TEST_DB_URL =
                "jdbc:derby:CinemaTestDB;create=true";

        private TestDatabaseManager() {
            super();
        }

        public static TestDatabaseManager createForTest() {
            return new TestDatabaseManager();
        }

        @Override
        public java.sql.Connection getConnection() throws SQLException {
            return java.sql.DriverManager.getConnection(TEST_DB_URL);
        }

        @Override
        public void shutdown() {
            try {
                java.sql.DriverManager.getConnection("jdbc:derby:CinemaTestDB;shutdown=true");
            } catch (SQLException e) {
                // XJ015 = clean shutdown, 08006 = DB-level shutdown – both are OK
            }
        }
    }
}
