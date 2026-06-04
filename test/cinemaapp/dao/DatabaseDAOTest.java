package cinemaapp.dao;

import cinemaapp.dao.derby.DerbyAccountDAO;
import cinemaapp.dao.derby.DerbyBookingDAO;
import cinemaapp.dao.derby.DerbyShowSeatDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.*;

import org.junit.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class DatabaseDAOTest {

    private static final String TEST_DB_URL =
            "jdbc:derby:AUTCinemaTestDB;create=true;user=comp603;password=comp603";

    private static Connection conn;
    private static DerbyBookingDAO  bookingDAO;
    private static DerbyAccountDAO  accountDAO;
    private static DerbyShowSeatDAO showSeatDAO;

   

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Load the embedded Derby driver
        try { Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver"); }
        catch (ClassNotFoundException ignored) {}

        conn = DriverManager.getConnection(TEST_DB_URL);
        conn.setAutoCommit(true);

        // Inject our test connection into the DatabaseManager Singleton so the
        // DAOs talk to AUTCinemaTestDB instead of the production database.
        DatabaseManager dm = DatabaseManager.getInstance();
        Field connField = DatabaseManager.class.getDeclaredField("connection");
        connField.setAccessible(true);
        connField.set(dm, conn);

        bookingDAO  = new DerbyBookingDAO(dm);
        accountDAO  = new DerbyAccountDAO(dm);
        showSeatDAO = new DerbyShowSeatDAO(dm);

        createSchema();
        seedMinimalData();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        dropSchema();
        conn.close();
        try {
            DriverManager.getConnection(
                "jdbc:derby:AUTCinemaTestDB;shutdown=true;user=comp603;password=comp603");
        } catch (SQLException e) {
            if (!"08006".equals(e.getSQLState())) throw e; // 08006 = clean per-DB shutdown
        }
    }

    /** Clear booking rows between tests so each test gets a clean slate. */
    @After
    public void cleanBookings() throws Exception {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM BookingItem");
            st.execute("DELETE FROM Booking");
        }
        conn.commit();
        conn.setAutoCommit(true);
    }

    // Test 1: save a booking and read it back 

    @Test
    public void testSaveAndFindBookingByCode() throws Exception {
        Booking booking = new Booking("BK-DB-001", "ST001");
        booking.addBookingItem(new BookingItem("SC001-A01", AttendeeType.ADULT, 25.00));
        booking.calculateTotalPrice();

        bookingDAO.save(booking, "testuser");

        Booking found = bookingDAO.findByBookingCode("BK-DB-001");

        assertNotNull("Booking should be retrievable after save", found);
        assertEquals("BK-DB-001", found.getBookingCode());
        assertEquals(1, found.getBookingItems().size());
        assertEquals("SC001-A01", found.getBookingItems().get(0).getSeatId());
        assertEquals(AttendeeType.ADULT, found.getBookingItems().get(0).getAttendeeType());
        assertEquals(25.00, found.getBookingItems().get(0).getItemPrice(), 0.001);
    }

    // Test 2: findByUsername returns all bookings for that user 

    @Test
    public void testFindByUsernameReturnsCorrectCount() throws Exception {
        Booking b1 = new Booking("BK-DB-002", "ST001");
        b1.addBookingItem(new BookingItem("SC001-A02", AttendeeType.ADULT, 25.00));
        b1.calculateTotalPrice();

        Booking b2 = new Booking("BK-DB-003", "ST001");
        b2.addBookingItem(new BookingItem("SC001-A03", AttendeeType.STUDENT, 18.75));
        b2.calculateTotalPrice();

        bookingDAO.save(b1, "testuser");
        bookingDAO.save(b2, "testuser");

        List<Booking> results = bookingDAO.findByUsername("testuser");
        assertEquals("testuser should have exactly 2 bookings", 2, results.size());
    }

    // Test 3: delete removes booking and its items 

    @Test
    public void testDeleteBookingRemovesItFromDB() throws Exception {
        Booking booking = new Booking("BK-DB-004", "ST001");
        booking.addBookingItem(new BookingItem("SC001-A04", AttendeeType.CHILD, 15.00));
        booking.calculateTotalPrice();

        bookingDAO.save(booking, "testuser");
        assertTrue("Booking should exist before delete",
                bookingDAO.existsByBookingCode("BK-DB-004"));

        bookingDAO.delete("BK-DB-004");

        assertFalse("Booking should not exist after delete",
                bookingDAO.existsByBookingCode("BK-DB-004"));
        assertNull("findByBookingCode should return null after delete",
                bookingDAO.findByBookingCode("BK-DB-004"));
    }

    // Test 4: seat status update persists to the database 

    @Test
    public void testUpdateSeatStatusPersists() throws Exception {
        Map<String, SeatStatus> before = showSeatDAO.findByShowtimeId("ST001");
        assertEquals("Seat SC001-A01 should start AVAILABLE",
                SeatStatus.AVAILABLE, before.get("SC001-A01"));

        showSeatDAO.updateSeatStatus("ST001", "SC001-A01", SeatStatus.BOOKED);

        Map<String, SeatStatus> after = showSeatDAO.findByShowtimeId("ST001");
        assertEquals("Seat SC001-A01 should be BOOKED after update",
                SeatStatus.BOOKED, after.get("SC001-A01"));

        // Restore so other tests see a clean ShowSeat state
        showSeatDAO.updateSeatStatus("ST001", "SC001-A01", SeatStatus.AVAILABLE);
    }

    // Test 5: account authentication 

    @Test
    public void testAuthenticateCorrectCredentials() throws Exception {
        Account account = accountDAO.authenticate("testuser", "testpass");
        assertNotNull("authenticate() should return an Account for valid credentials", account);
        assertEquals("testuser", account.getUsername());
    }

    @Test
    public void testAuthenticateWrongPasswordReturnsNull() throws Exception {
        Account account = accountDAO.authenticate("testuser", "wrongpass");
        assertNull("authenticate() should return null for wrong password", account);
    }

    // Schema helpers 

    private static void createSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            for (String sql : new String[]{
                "CREATE TABLE Account (username VARCHAR(50) NOT NULL PRIMARY KEY, password VARCHAR(50) NOT NULL)",
                "CREATE TABLE Movie (movieId VARCHAR(10) NOT NULL PRIMARY KEY, title VARCHAR(50) NOT NULL, rating VARCHAR(20) NOT NULL, description VARCHAR(255) NOT NULL, runtime INTEGER NOT NULL)",
                "CREATE TABLE Screen (screenId VARCHAR(10) NOT NULL PRIMARY KEY)",
                "CREATE TABLE Seat (seatId VARCHAR(20) NOT NULL PRIMARY KEY, row CHAR(1) NOT NULL, number INTEGER NOT NULL, nearAisle INTEGER NOT NULL, isAccessible INTEGER NOT NULL, screenId VARCHAR(10) NOT NULL, CONSTRAINT fk_t_seat_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId))",
                "CREATE TABLE Showtime (showtimeId VARCHAR(10) NOT NULL PRIMARY KEY, dateTime TIMESTAMP NOT NULL, basePrice FLOAT NOT NULL, screenId VARCHAR(10) NOT NULL, movieId VARCHAR(10) NOT NULL, CONSTRAINT fk_t_st_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId), CONSTRAINT fk_t_st_movie FOREIGN KEY (movieId) REFERENCES Movie(movieId))",
                "CREATE TABLE ShowSeat (seatId VARCHAR(20) NOT NULL, showtimeId VARCHAR(10) NOT NULL, seatStatus VARCHAR(20) NOT NULL, PRIMARY KEY (seatId, showtimeId), CONSTRAINT fk_t_ss_seat FOREIGN KEY (seatId) REFERENCES Seat(seatId), CONSTRAINT fk_t_ss_show FOREIGN KEY (showtimeId) REFERENCES Showtime(showtimeId))",
                "CREATE TABLE Booking (bookingCode VARCHAR(20) NOT NULL PRIMARY KEY, totalPrice FLOAT NOT NULL, username VARCHAR(50) NOT NULL, showtimeId VARCHAR(10) NOT NULL, CONSTRAINT fk_t_b_acc FOREIGN KEY (username) REFERENCES Account(username), CONSTRAINT fk_t_b_show FOREIGN KEY (showtimeId) REFERENCES Showtime(showtimeId))",
                "CREATE TABLE BookingItem (bookingCode VARCHAR(20) NOT NULL, seatId VARCHAR(20) NOT NULL, itemPrice FLOAT NOT NULL, attendeeType VARCHAR(20) NOT NULL, PRIMARY KEY (bookingCode, seatId), CONSTRAINT fk_t_bi_book FOREIGN KEY (bookingCode) REFERENCES Booking(bookingCode), CONSTRAINT fk_t_bi_seat FOREIGN KEY (seatId) REFERENCES Seat(seatId))"
            }) {
                try { st.execute(sql); }
                catch (SQLException e) {
                    if (!"X0Y32".equals(e.getSQLState())) throw e; // X0Y32 = table already exists
                }
            }
        }
    }

    private static void seedMinimalData() throws SQLException {
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {
            st.execute("INSERT INTO Account VALUES ('testuser', 'testpass')");
            st.execute("INSERT INTO Movie VALUES ('M001','Test Movie','PG','A test.',120)");
            st.execute("INSERT INTO Screen VALUES ('SC001')");
            for (int i = 1; i <= 5; i++) {
                String seatId = String.format("SC001-A%02d", i);
                st.execute(String.format(
                    "INSERT INTO Seat VALUES ('%s','A',%d,0,0,'SC001')", seatId, i));
            }
            st.execute("INSERT INTO Showtime VALUES ('ST001','2026-06-10 10:00:00',25,'SC001','M001')");
            for (int i = 1; i <= 5; i++) {
                String seatId = String.format("SC001-A%02d", i);
                st.execute(String.format(
                    "INSERT INTO ShowSeat VALUES ('%s','ST001','AVAILABLE')", seatId));
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void dropSchema() {
        String[] tables = {"BookingItem","Booking","ShowSeat","Showtime","Seat","Screen","Movie","Account"};
        try (Statement st = conn.createStatement()) {
            for (String t : tables) {
                try { st.execute("DROP TABLE " + t); }
                catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
    }
}