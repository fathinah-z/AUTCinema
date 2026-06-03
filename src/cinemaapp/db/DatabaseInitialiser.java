package cinemaapp.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates all tables (if they do not already exist) and seeds initial data.
 *
 * Call {@link #initialise()} once at application start-up, after obtaining the
 * {@link DatabaseManager} instance.
 *
 * Schema mirrors the ERD exactly:
 *
 * <pre>
 *   Account      (username PK, password)
 *   Movie        (movieId PK, title, rating, description, runtime)
 *   Screen       (screenId PK)
 *   Seat         (seatId PK, row, number, nearAisle, isAccessible, screenId FK)
 *   Showtime     (showtimeId PK, dateTime, basePrice, screenId FK, movieId FK)
 *   ShowSeat     (seatId FK, showtimeId FK, seatStatus)  – composite PK
 *   Booking      (bookingCode PK, bookingDate, totalPrice, username FK)
 *   BookingItem  (itemPrice, attendeeType, bookingCode FK, seatId FK)  – composite PK
 * </pre>
 */
public class DatabaseInitialiser {

    private final DatabaseManager dbManager;

    public DatabaseInitialiser(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** Entry point – idempotent; safe to call every startup. */
    public void initialise() throws SQLException {
        createTables();
        if (isEmpty()) {
            seedData();
        }
    }

    // -----------------------------------------------------------------------
    // Table creation
    // -----------------------------------------------------------------------

    private void createTables() throws SQLException {
        Connection conn = dbManager.getConnection();
        
        try (Statement st = conn.createStatement()) {
    for (String sql : new String[]{
        "CREATE TABLE Account ("
                + "  username  VARCHAR(50) NOT NULL PRIMARY KEY, "
                + "  password  VARCHAR(50) NOT NULL"
                + ")",
        "CREATE TABLE Movie ("
                + "  movieId     VARCHAR(10)   NOT NULL PRIMARY KEY, "
                + "  title       VARCHAR(50)  NOT NULL, "
                + "  rating      VARCHAR(20)  NOT NULL, "
                + "  description VARCHAR(255) NOT NULL, "
                + "  runtime     INTEGER      NOT NULL"
                + ")",
        "CREATE TABLE Screen ("
                + "  screenId VARCHAR(10) NOT NULL PRIMARY KEY"
                + ")",
        "CREATE TABLE Seat ("
                + "  seatId       VARCHAR(10)  NOT NULL PRIMARY KEY, "
                + "  row          CHAR(1)     NOT NULL, "
                + "  number       INTEGER     NOT NULL, "
                + "  nearAisle    INTEGER     NOT NULL, "   // 0 / 1 for boolean
                + "  isAccessible INTEGER     NOT NULL, "
                + "  screenId     VARCHAR(10)  NOT NULL, "
                + "  CONSTRAINT fk_seat_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId)"
                + ")",
        "CREATE TABLE Showtime ("
                + "  showtimeId VARCHAR(10)   NOT NULL PRIMARY KEY, "
                + "  dateTime   TIMESTAMP    NOT NULL, "
                + "  basePrice  FLOAT        NOT NULL, "
                + "  screenId   VARCHAR(10)   NOT NULL, "
                + "  movieId    VARCHAR(10)   NOT NULL, "
                + "  CONSTRAINT fk_showtime_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId), "
                + "  CONSTRAINT fk_showtime_movie  FOREIGN KEY (movieId)  REFERENCES Movie(movieId)"
                + ")",
        "CREATE TABLE ShowSeat ("
                + "  seatId      VARCHAR(10)  NOT NULL, "
                + "  showtimeId  VARCHAR(10)  NOT NULL, "
                + "  seatStatus  VARCHAR(20) NOT NULL, "
                + "  PRIMARY KEY (seatId, showtimeId), "
                + "  CONSTRAINT fk_showseat_seat     FOREIGN KEY (seatId)     REFERENCES Seat(seatId), "
                + "  CONSTRAINT fk_showseat_showtime FOREIGN KEY (showtimeId) REFERENCES Showtime(showtimeId)"
                + ")",
        "CREATE TABLE Booking ("
                + "  bookingCode  VARCHAR(20) NOT NULL PRIMARY KEY, "
                + "  bookingDate  DATE        NOT NULL, "
                + "  totalPrice   FLOAT       NOT NULL, "
                + "  username     VARCHAR(50) NOT NULL, "
                + "  CONSTRAINT fk_booking_account FOREIGN KEY (username) REFERENCES Account(username)"
                + ")",
        "CREATE TABLE BookingItem ("
                + "  bookingCode  VARCHAR(20) NOT NULL, "
                + "  seatId       VARCHAR(10)  NOT NULL, "
                + "  itemPrice    FLOAT       NOT NULL, "
                + "  attendeeType VARCHAR(20) NOT NULL, "
                + "  PRIMARY KEY (bookingCode, seatId), "
                + "  CONSTRAINT fk_item_booking FOREIGN KEY (bookingCode) REFERENCES Booking(bookingCode), "
                + "  CONSTRAINT fk_item_seat    FOREIGN KEY (seatId)      REFERENCES Seat(seatId)"
                + ")"
    }) {
        try {
            st.execute(sql);
        } catch (SQLException e) {
            if (!"X0Y32".equals(e.getSQLState())) throw e;
            // else: table already exists, skip it
        }
    }
}
    }

    // -----------------------------------------------------------------------
    // Seed data check
    // -----------------------------------------------------------------------

    /** Returns true when the Movie table already has rows. */
    private boolean isEmpty() throws SQLException {
        Connection conn = dbManager.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Movie")) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    // -----------------------------------------------------------------------
    // Seed data
    // -----------------------------------------------------------------------

    private void seedData() throws SQLException {
        Connection conn = dbManager.getConnection();
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {

            // --- Accounts ---
            st.execute("INSERT INTO Account VALUES ('admin',  'admin123')");
            st.execute("INSERT INTO Account VALUES ('guest',  'guest')");

            // --- Movies ---
            st.execute("INSERT INTO Movie VALUES ('M01','Interstellar','PG','A team of explorers travel through a wormhole in space.',169)");
            st.execute("INSERT INTO Movie VALUES ('M02','The Dark Knight','M','Batman fights the Joker, a criminal mastermind.',152)");
            st.execute("INSERT INTO Movie VALUES ('M03','Parasite','R16','A poor family scheme to become employed by a wealthy family.',132)");
            st.execute("INSERT INTO Movie VALUES ('M04','Toy Story','G','A cowboy doll is threatened by a space action figure.',81)");
            st.execute("INSERT INTO Movie VALUES ('M05','Inception','M','A thief who steals secrets through dreams is given a heist task.',148)");

            // --- Screens ---
            st.execute("INSERT INTO Screen VALUES ('S01')");
            st.execute("INSERT INTO Screen VALUES ('S02')");

            // --- Seats for Screen S01 (rows A-E, 10 seats per row) ---
            seedSeats(st, "S01", 'A', 'E', 10);

            // --- Seats for Screen S02 (rows A-D, 8 seats per row) ---
            // seedSeats(st, "S02", 'A', 'D', 8);

            // --- Showtimes ---
            st.execute("INSERT INTO Showtime VALUES ('T01','2026-06-10 10:00:00',14.50,'S01','M01')");
            st.execute("INSERT INTO Showtime VALUES ('T02','2026-06-10 14:30:00',14.50,'S01','M01')");
            st.execute("INSERT INTO Showtime VALUES ('T03','2026-06-11 18:00:00',16.00,'S02','M02')");
            st.execute("INSERT INTO Showtime VALUES ('T04','2026-06-12 11:00:00',12.00,'S01','M03')");
            st.execute("INSERT INTO Showtime VALUES ('T05','2026-06-13 15:00:00',10.00,'S02','M04')");
            st.execute("INSERT INTO Showtime VALUES ('T06','2026-06-14 20:00:00',16.00,'S01','M05')");

            // --- ShowSeats (all seats start as AVAILABLE) ---
            seedShowSeats(st, "T01", "S01", 'A', 'E', 10);
            //seedShowSeats(st, "T02", "S01", 'A', 'E', 10);
            seedShowSeats(st, "T03", "S02", 'A', 'D', 8);
            //seedShowSeats(st, "T04", "S01", 'A', 'E', 10);
            //seedShowSeats(st, "T05", "S02", 'A', 'D', 8);
            //seedShowSeats(st, "T06", "S01", 'A', 'E', 10);

            conn.commit();
            System.out.println("Database seeded successfully.");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Inserts all seats for a given screen into the Seat table.
     * Aisle and accessible logic mirrors {@code FileScreenRepository.buildScreen()}.
     */
    private void seedSeats(Statement st, String screenId,
                           char firstRow, char lastRow,
                           int seatsPerRow) throws SQLException {
        int middle = seatsPerRow / 2;
        for (char row = firstRow; row <= lastRow; row++) {
            for (int i = 1; i <= seatsPerRow; i++) {
                // seatId is at most 10 chars: e.g. "A01" – VARCHAR(3) is fine
                String seatId = row + String.format("%02d", i);
                int nearAisle = (i == 1 || i == middle || i == middle + 1 || i == seatsPerRow) ? 1 : 0;
                int accessible = (row == lastRow
                        && (i == middle - 1 || i == middle || i == middle + 1 || i == middle + 2)) ? 1 : 0;
                st.execute(String.format(
                    "INSERT INTO Seat VALUES ('%s','%c',%d,%d,%d,'%s')",
                    seatId, row, i, nearAisle, accessible, screenId));
            }
        }
    }

    /**
     * Inserts a ShowSeat row (AVAILABLE) for every seat in a showtime.
     */
    private void seedShowSeats(Statement st, String showtimeId,
                               String screenId,
                               char firstRow, char lastRow,
                               int seatsPerRow) throws SQLException {
        for (char row = firstRow; row <= lastRow; row++) {
            for (int i = 1; i <= seatsPerRow; i++) {
                String seatId = row + String.format("%02d", i);
                st.execute(String.format(
                    "INSERT INTO ShowSeat VALUES ('%s','%s','AVAILABLE')",
                    seatId, showtimeId));
            }
        }
    }
}