package cinemaapp.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates all tables and seeds initial data.
 *
 * Call {@link #initialise()} once at application start-up, after obtaining the
 * {@link DatabaseManager} instance.
 *
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

    //entrypoint
    public void initialise() throws SQLException {
        createTables();
        if (isEmpty()) {
            seedData();
        }
    }

    // table creation
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
                + "  seatId       VARCHAR(20)  NOT NULL PRIMARY KEY, "
                + "  row          CHAR(1)     NOT NULL, "
                + "  number       INTEGER     NOT NULL, "
                + "  nearAisle    INTEGER     NOT NULL, " // 0 / 1 for boolean
                + "  isAccessible INTEGER     NOT NULL, "
                + "  screenId     VARCHAR(10)  NOT NULL, "
                + "  CONSTRAINT fk_seat_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId) "
                + ")",
                "CREATE TABLE Showtime ("
                + "  showtimeId VARCHAR(10)   NOT NULL PRIMARY KEY, "
                + "  dateTime   TIMESTAMP    NOT NULL, "
                + "  basePrice  FLOAT        NOT NULL, "
                + "  screenId   VARCHAR(10)   NOT NULL, "
                + "  movieId    VARCHAR(10)   NOT NULL, "
                + "  CONSTRAINT fk_showtime_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId), "
                + "  CONSTRAINT fk_showtime_movie  FOREIGN KEY (movieId)  REFERENCES Movie(movieId) "
                + ")",
                "CREATE TABLE ShowSeat ("
                + "  seatId      VARCHAR(20)  NOT NULL, "
                + "  showtimeId  VARCHAR(10)  NOT NULL, "
                + "  seatStatus  VARCHAR(20) NOT NULL, "
                + "  PRIMARY KEY (seatId, showtimeId), "
                + "  CONSTRAINT fk_showseat_seat     FOREIGN KEY (seatId)     REFERENCES Seat(seatId), "
                + "  CONSTRAINT fk_showseat_showtime FOREIGN KEY (showtimeId) REFERENCES Showtime(showtimeId) "
                + ")",
                "CREATE TABLE Booking ("
                + "  bookingCode  VARCHAR(20) NOT NULL PRIMARY KEY, "
                + "  bookingDate  DATE        NOT NULL, "
                + "  totalPrice   FLOAT       NOT NULL, "
                + "  username     VARCHAR(50) NOT NULL, "
                + "  CONSTRAINT fk_booking_account FOREIGN KEY (username) REFERENCES Account(username) "
                + ")",
                "CREATE TABLE BookingItem ("
                + "  bookingCode  VARCHAR(20) NOT NULL, "
                + "  seatId       VARCHAR(20)  NOT NULL, "
                + "  itemPrice    FLOAT       NOT NULL, "
                + "  attendeeType VARCHAR(20) NOT NULL, "
                + "  PRIMARY KEY (bookingCode, seatId), "
                + "  CONSTRAINT fk_item_booking FOREIGN KEY (bookingCode) REFERENCES Booking(bookingCode), "
                + "  CONSTRAINT fk_item_seat    FOREIGN KEY (seatId)     REFERENCES Seat(seatId) "
                + ")"
            }) {
                try {
                    st.execute(sql);
                } catch (SQLException e) {
                    if (!"X0Y32".equals(e.getSQLState())) {
                        throw e;
                    }
                    //skip table if exits already
                }
            }
        }
    }

    //seed data check
    private boolean isEmpty() throws SQLException {
        Connection conn = dbManager.getConnection();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Movie")) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    //seed data
    private void seedData() throws SQLException {
        Connection conn = dbManager.getConnection();
        conn.setAutoCommit(false);
        try (Statement st = conn.createStatement()) {

            //  Accounts 
            st.execute("INSERT INTO Account VALUES ('admin', 'admin123')");
            st.execute("INSERT INTO Account VALUES ('guest', 'guest')");

            //  Movies 
            /*
            M001|Project Hail Mary|PG|A science teacher wakes up alone on a spaceship. As his memory returns, he uncovers a mission to stop a mysterious substance killing Earth's sun and that an unexpected friendship may be the key.|157
            M002|The Handmaiden|R18|In 1930s Korea, a girl is hired as a handmaiden to a Japanese heiress who lives a secluded life on a countryside estate. But the maid has a secret: She is a pickpocket recruited by a swindler to help seduce the Lady and steal her fortune.|145
            M003|Forrest Gump|R13|The history of the United States from the 1950s to the '70s unfolds from the perspective of an Alabama man with an IQ of 75, who yearns to be reunited with his childhood sweetheart.|142
            M004|Fight Club|R16|An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.|139
             */
            st.execute("INSERT INTO Movie VALUES ('M001','Project Hail Mary','PG','A science teacher wakes up alone on a spaceship. As his memory returns, he uncovers a mission to stop a mysterious substance killing the Sun and that an unexpected friendship may be the key.',157)");
            st.execute("INSERT INTO Movie VALUES ('M002','The Handmaiden','R18','In 1930s Korea, a girl is hired as a handmaiden to a Japanese heiress who lives a secluded life on a countryside estate. But the maid has a secret: She is a pickpocket recruited by a swindler to help seduce the Lady and steal her fortune.',145)");
            st.execute("INSERT INTO Movie VALUES ('M003','Forrest Gump','R13','The history of the United States from the 1950s to the 1970s unfolds from the perspective of an Alabama man with an IQ of 75, who yearns to be reunited with his childhood sweetheart.',142)");
            st.execute("INSERT INTO Movie VALUES ('M004','Fight Club','R16','An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.',139)");

            //  Screens 
            st.execute("INSERT INTO Screen VALUES ('SC001')");
            st.execute("INSERT INTO Screen VALUES ('SC002')");

            //  Seats: S01 = rows A-E, 10 per row | S02 = rows A-D, 8 per row 
            seedSeats(st, "SC001", 'A', 'E', 10);
            seedSeats(st, "SC002", 'A', 'D', 8);

            //  Showtimes 
            /*
            ST001|M001|SC001|2026-08-19T15:00|25
            ST002|M003|SC002|2026-08-19T15:00|25
            ST003|M002|SC001|2026-08-20T13:00|22
            ST004|M001|SC001|2026-08-20T18:00|30
            ST005|M004|SC002|2026-08-21T12:30|22
            */
            st.execute("INSERT INTO Showtime VALUES ('ST001','2026-06-10 10:00:00',25,'SC001','M001')");
            st.execute("INSERT INTO Showtime VALUES ('ST002','2026-06-10 14:30:00',25,'SC002','M003')");
            st.execute("INSERT INTO Showtime VALUES ('ST003','2026-06-11 18:00:00',22,'SC001','M002')");
            st.execute("INSERT INTO Showtime VALUES ('ST004','2026-06-12 11:00:00',30,'SC001','M001')");
            st.execute("INSERT INTO Showtime VALUES ('ST005','2026-06-13 15:00:00',22,'SC002','M004')");

            //  ShowSeats: every showtime gets all seats from its screen as AVAILABLE 
            // ST001, ST003, ST004 use SC001 | ST002, ST005 use SC002
            seedShowSeats(st, "ST001",  "SC001", 'A', 'E', 10);
            seedShowSeats(st, "ST002",  "SC002", 'A', 'D', 8);
            seedShowSeats(st, "ST003",  "SC001", 'A', 'E', 10);
            seedShowSeats(st, "ST004",  "SC001", 'A', 'E', 10);
            seedShowSeats(st, "ST005",  "SC002", 'A', 'D', 8);

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
     * Inserts all seats for a given screen into the Seat table. Aisle and
     * accessible logic mirrors {@code FileScreenRepository.buildScreen()}.
     */
    private void seedSeats(Statement st, String screenId,
            char firstRow, char lastRow,
            int seatsPerRow) throws SQLException {
        int middle = seatsPerRow / 2;
        for (char row = firstRow; row <= lastRow; row++) {
            for (int i = 1; i <= seatsPerRow; i++) {
                String seatId = screenId+"-"+row + String.format("%02d", i);
                int nearAisle = (i == 1 || i == middle || i == middle + 1 || i == seatsPerRow) ? 1 : 0;
                int accessible = (row == lastRow
                        && (i == middle - 1 || i == middle || i == middle + 1 || i == middle + 2)) ? 1 : 0;
                try {
                    st.execute(String.format(
                            "INSERT INTO Seat VALUES ('%s','%c',%d,%d,%d,'%s')",
                            seatId, row, i, nearAisle, accessible, screenId));
                } catch (SQLException e) {
                    if (!"23505".equals(e.getSQLState()) && !"23000".equals(e.getSQLState())) {
                        throw e;
                    }
                    // duplicate key — seat already exists, skip it
                }
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
                String seatId = screenId+"-"+row + String.format("%02d", i);
                st.execute(String.format(
                        "INSERT INTO ShowSeat VALUES ('%s', '%s','AVAILABLE')",
                        seatId, showtimeId));
            }
        }
    }
}
