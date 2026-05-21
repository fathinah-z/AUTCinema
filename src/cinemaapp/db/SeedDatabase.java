package cinemaapp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Standalone database seeder.
 *
 * Run this class ONCE before starting the GUI to create and populate the
 * embedded Derby database (AUTCinemaDB/) with data that exactly matches
 * the project txt files:
 *
 *   movies.txt    → Movie table
 *   screens.txt   → Screen + Seat tables (seat layout via buildScreen logic)
 *   showtimes.txt → Showtime table
 *   showseats.txt → ShowSeat table  (statuses copied verbatim)
 *   bookings.txt  → Booking + BookingItem tables
 *
 * Safe to run more than once — every insert is guarded by an existence check.
 *
 * HOW TO RUN IN NETBEANS:
 *   Right-click this file → Run File
 *
 * The AUTCinemaDB/ folder is created in the NetBeans project working
 * directory (the project root). No other configuration is required.
 */
public class SeedDatabase {

    private static final String DB_URL = "jdbc:derby:AUTCinemaDB;user=comp603;password=comp603";

    public static void main(String[] args) {
        System.out.println("=== Cinema Booking System – Database Seeder ===");

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            createTables(conn);
            seedAccounts(conn);
            seedMovies(conn);
            seedScreens(conn);
            seedSeats(conn);
            seedShowtimes(conn);
            seedShowSeats(conn);
            seedBookings(conn);

            conn.commit();
            System.out.println("\n✓ All done. Database is ready.");

        } catch (SQLException e) {
            System.err.println("Seeding failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownDerby();
        }
    }

    // -----------------------------------------------------------------------
    // Table creation
    // -----------------------------------------------------------------------

    private static void createTables(Connection conn) throws SQLException {
        System.out.println("\n[1/8] Creating tables...");
        try (Statement st = conn.createStatement()) {
            createIfAbsent(st, "Account",
                "CREATE TABLE Account ("
                + "  username VARCHAR(50) NOT NULL PRIMARY KEY,"
                + "  password VARCHAR(50) NOT NULL"
                + ")"
            );
            createIfAbsent(st, "Movie",
                "CREATE TABLE Movie ("
                + "  movieId     VARCHAR(10)  NOT NULL PRIMARY KEY,"
                + "  title       VARCHAR(100) NOT NULL,"
                + "  rating      VARCHAR(20)  NOT NULL,"
                + "  description VARCHAR(500) NOT NULL,"
                + "  runtime     INTEGER      NOT NULL"
                + ")"
            );
            createIfAbsent(st, "Screen",
                "CREATE TABLE Screen ("
                + "  screenId VARCHAR(10) NOT NULL PRIMARY KEY"
                + ")"
            );
            createIfAbsent(st, "Seat",
                "CREATE TABLE Seat ("
                + "  seatId       VARCHAR(10) NOT NULL PRIMARY KEY,"
                + "  row          CHAR(1)     NOT NULL,"
                + "  number       INTEGER     NOT NULL,"
                + "  nearAisle    INTEGER     NOT NULL,"
                + "  isAccessible INTEGER     NOT NULL,"
                + "  screenId     VARCHAR(10) NOT NULL,"
                + "  CONSTRAINT fk_seat_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId)"
                + ")"
            );
            createIfAbsent(st, "Showtime",
                "CREATE TABLE Showtime ("
                + "  showtimeId VARCHAR(10) NOT NULL PRIMARY KEY,"
                + "  dateTime   TIMESTAMP   NOT NULL,"
                + "  basePrice  FLOAT       NOT NULL,"
                + "  screenId   VARCHAR(10) NOT NULL,"
                + "  movieId    VARCHAR(10) NOT NULL,"
                + "  CONSTRAINT fk_showtime_screen FOREIGN KEY (screenId) REFERENCES Screen(screenId),"
                + "  CONSTRAINT fk_showtime_movie  FOREIGN KEY (movieId)  REFERENCES Movie(movieId)"
                + ")"
            );
            createIfAbsent(st, "ShowSeat",
                "CREATE TABLE ShowSeat ("
                + "  seatId     VARCHAR(10) NOT NULL,"
                + "  showtimeId VARCHAR(10) NOT NULL,"
                + "  seatStatus VARCHAR(20) NOT NULL,"
                + "  PRIMARY KEY (seatId, showtimeId),"
                + "  CONSTRAINT fk_showseat_seat     FOREIGN KEY (seatId)     REFERENCES Seat(seatId),"
                + "  CONSTRAINT fk_showseat_showtime FOREIGN KEY (showtimeId) REFERENCES Showtime(showtimeId)"
                + ")"
            );
            createIfAbsent(st, "Booking",
                "CREATE TABLE Booking ("
                + "  bookingCode VARCHAR(20) NOT NULL PRIMARY KEY,"
                + "  bookingDate DATE        NOT NULL,"
                + "  totalPrice  FLOAT       NOT NULL,"
                + "  username    VARCHAR(50) NOT NULL,"
                + "  CONSTRAINT fk_booking_account FOREIGN KEY (username) REFERENCES Account(username)"
                + ")"
            );
            createIfAbsent(st, "BookingItem",
                "CREATE TABLE BookingItem ("
                + "  bookingCode  VARCHAR(20) NOT NULL,"
                + "  seatId       VARCHAR(10) NOT NULL,"
                + "  itemPrice    FLOAT       NOT NULL,"
                + "  attendeeType VARCHAR(20) NOT NULL,"
                + "  PRIMARY KEY (bookingCode, seatId),"
                + "  CONSTRAINT fk_item_booking FOREIGN KEY (bookingCode) REFERENCES Booking(bookingCode),"
                + "  CONSTRAINT fk_item_seat    FOREIGN KEY (seatId)      REFERENCES Seat(seatId)"
                + ")"
            );
        }
        System.out.println("   Tables ready.");
    }

    // -----------------------------------------------------------------------
    // Accounts  (no accounts in txt files – seed sensible defaults)
    // -----------------------------------------------------------------------

    private static void seedAccounts(Connection conn) throws SQLException {
        System.out.println("\n[2/8] Seeding accounts...");
        String sql = "INSERT INTO Account (username, password) VALUES (?, ?)";

        String[][] accounts = {
            {"admin", "admin123"},
            {"guest", "guest"}
        };

        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String[] a : accounts) {
                if (!exists(conn, "Account", "username", a[0])) {
                    ps.setString(1, a[0]);
                    ps.setString(2, a[1]);
                    ps.executeUpdate();
                    System.out.println("   + " + a[0]);
                    n++;
                } else {
                    System.out.println("   ~ already exists: " + a[0]);
                }
            }
        }
        System.out.println("   " + n + " account(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Movies  (from movies.txt)
    // M001|Project Hail Mary|PG|...|157
    // M002|The Handmaiden|R18|...|145
    // M003|Forrest Gump|R13|...|142
    // M004|Fight Club|R16|...|139
    // -----------------------------------------------------------------------

    private static void seedMovies(Connection conn) throws SQLException {
        System.out.println("\n[3/8] Seeding movies...");
        String sql = "INSERT INTO Movie (movieId, title, rating, description, runtime) VALUES (?,?,?,?,?)";

        Object[][] movies = {
            {"M001", "Project Hail Mary", "PG",
             "A science teacher wakes up alone on a spaceship. As his memory returns, he uncovers a mission to stop a mysterious substance killing Earth's sun and that an unexpected friendship may be the key.",
             157},
            {"M002", "The Handmaiden", "R18",
             "In 1930s Korea, a girl is hired as a handmaiden to a Japanese heiress who lives a secluded life on a countryside estate. But the maid has a secret: She is a pickpocket recruited by a swindler to help seduce the Lady and steal her fortune.",
             145},
            {"M003", "Forrest Gump", "R13",
             "The history of the United States from the 1950s to the '70s unfolds from the perspective of an Alabama man with an IQ of 75, who yearns to be reunited with his childhood sweetheart.",
             142},
            {"M004", "Fight Club", "R16",
             "An insomniac office worker and a devil-may-care soap maker form an underground fight club that evolves into much more.",
             139},
        };

        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] m : movies) {
                if (!exists(conn, "Movie", "movieId", (String) m[0])) {
                    ps.setString(1, (String) m[0]);
                    ps.setString(2, (String) m[1]);
                    ps.setString(3, (String) m[2]);
                    ps.setString(4, (String) m[3]);
                    ps.setInt(5, (int) m[4]);
                    ps.executeUpdate();
                    System.out.println("   + " + m[0] + " – " + m[1]);
                    n++;
                } else {
                    System.out.println("   ~ already exists: " + m[0]);
                }
            }
        }
        System.out.println("   " + n + " movie(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Screens  (from screens.txt)
    // SC001|A|E|10
    // SC002|A|D|8
    // -----------------------------------------------------------------------

    private static void seedScreens(Connection conn) throws SQLException {
        System.out.println("\n[4/8] Seeding screens...");
        String sql = "INSERT INTO Screen (screenId) VALUES (?)";

        String[] screens = {"SC001", "SC002"};

        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String id : screens) {
                if (!exists(conn, "Screen", "screenId", id)) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                    System.out.println("   + " + id);
                    n++;
                } else {
                    System.out.println("   ~ already exists: " + id);
                }
            }
        }
        System.out.println("   " + n + " screen(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Seats  (derived from screens.txt via buildScreen logic)
    // SC001: rows A-E, 10 seats per row  → 50 seats
    // SC002: rows A-D,  8 seats per row  → 32 seats
    //
    // nearAisle / isAccessible mirrors FileScreenRepository.buildScreen() exactly:
    //   nearAisle  = i==1 || i==middle || i==middle+1 || i==seatsPerRow
    //   accessible = row==lastRow && (i==middle-1 || i==middle || i==middle+1 || i==middle+2)
    // -----------------------------------------------------------------------

    private static void seedSeats(Connection conn) throws SQLException {
        System.out.println("\n[5/8] Seeding seats...");

        // {screenId, firstRow, lastRow, seatsPerRow}
        Object[][] layouts = {
            {"SC001", 'A', 'E', 10},
            {"SC002", 'A', 'D',  8},
        };

        String sql = "INSERT INTO Seat (seatId, row, number, nearAisle, isAccessible, screenId) VALUES (?,?,?,?,?,?)";

        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] layout : layouts) {
                String screenId    = (String) layout[0];
                char   firstRow    = (char)   layout[1];
                char   lastRow     = (char)   layout[2];
                int    seatsPerRow = (int)     layout[3];
                int    middle      = seatsPerRow / 2;

                for (char row = firstRow; row <= lastRow; row++) {
                    for (int i = 1; i <= seatsPerRow; i++) {
                        String seatId = row + String.format("%02d", i);

                        int nearAisle  = (i == 1 || i == middle || i == middle + 1 || i == seatsPerRow) ? 1 : 0;
                        int accessible = (row == lastRow
                                && (i == middle - 1 || i == middle || i == middle + 1 || i == middle + 2)) ? 1 : 0;

                        if (!exists(conn, "Seat", "seatId", seatId)) {
                            ps.setString(1, seatId);
                            ps.setString(2, String.valueOf(row));
                            ps.setInt(3, i);
                            ps.setInt(4, nearAisle);
                            ps.setInt(5, accessible);
                            ps.setString(6, screenId);
                            ps.executeUpdate();
                            n++;
                        }
                    }
                }
                System.out.println("   + " + screenId
                        + " (rows " + firstRow + "-" + lastRow
                        + ", " + seatsPerRow + " per row)");
            }
        }
        System.out.println("   " + n + " seat(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Showtimes  (from showtimes.txt)
    // ST001|M001|SC001|2026-08-19T15:00|25
    // ST002|M003|SC002|2026-08-19T15:00|25
    // ST003|M002|SC001|2026-08-20T13:00|22
    // ST004|M001|SC001|2026-08-20T18:00|30
    // ST005|M004|SC002|2026-08-21T12:30|22
    // -----------------------------------------------------------------------

    private static void seedShowtimes(Connection conn) throws SQLException {
        System.out.println("\n[6/8] Seeding showtimes...");
        String sql = "INSERT INTO Showtime (showtimeId, dateTime, basePrice, screenId, movieId) VALUES (?,?,?,?,?)";

        // {showtimeId, timestamp string, basePrice, screenId, movieId}
        Object[][] showtimes = {
            {"ST001", "2026-08-19 15:00:00", 25.0, "SC001", "M001"},
            {"ST002", "2026-08-19 15:00:00", 25.0, "SC002", "M003"},
            {"ST003", "2026-08-20 13:00:00", 22.0, "SC001", "M002"},
            {"ST004", "2026-08-20 18:00:00", 30.0, "SC001", "M001"},
            {"ST005", "2026-08-21 12:30:00", 22.0, "SC002", "M004"},
        };

        int n = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] st : showtimes) {
                if (!exists(conn, "Showtime", "showtimeId", (String) st[0])) {
                    ps.setString(1, (String) st[0]);
                    ps.setTimestamp(2, java.sql.Timestamp.valueOf((String) st[1]));
                    ps.setDouble(3, (double) st[2]);
                    ps.setString(4, (String) st[3]);
                    ps.setString(5, (String) st[4]);
                    ps.executeUpdate();
                    System.out.println("   + " + st[0] + " (" + st[4] + " @ " + st[1] + " $" + st[2] + ")");
                    n++;
                } else {
                    System.out.println("   ~ already exists: " + st[0]);
                }
            }
        }
        System.out.println("   " + n + " showtime(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // ShowSeats  (from showseats.txt – statuses copied verbatim)
    // -----------------------------------------------------------------------

    private static void seedShowSeats(Connection conn) throws SQLException {
        System.out.println("\n[7/8] Seeding show seats...");
        String sql = "INSERT INTO ShowSeat (seatId, showtimeId, seatStatus) VALUES (?,?,?)";
        String checkSql = "SELECT COUNT(*) FROM ShowSeat WHERE seatId = ? AND showtimeId = ?";

        // Each entry: {showtimeId, seatId, status}
        // Copied exactly from showseats.txt
        String[][] showSeats = {
            // ST001 – SC001 (rows A-E, 10 seats)
            {"ST001","A01","AVAILABLE"},{"ST001","A02","BOOKED"},  {"ST001","A03","BOOKED"},
            {"ST001","A04","AVAILABLE"},{"ST001","A05","AVAILABLE"},{"ST001","A06","AVAILABLE"},
            {"ST001","A07","AVAILABLE"},{"ST001","A08","AVAILABLE"},{"ST001","A09","AVAILABLE"},
            {"ST001","A10","AVAILABLE"},{"ST001","B01","AVAILABLE"},{"ST001","B02","AVAILABLE"},
            {"ST001","B03","AVAILABLE"},{"ST001","B04","AVAILABLE"},{"ST001","B05","AVAILABLE"},
            {"ST001","B06","AVAILABLE"},{"ST001","B07","AVAILABLE"},{"ST001","B08","AVAILABLE"},
            {"ST001","B09","AVAILABLE"},{"ST001","B10","AVAILABLE"},{"ST001","C01","AVAILABLE"},
            {"ST001","C02","AVAILABLE"},{"ST001","C03","AVAILABLE"},{"ST001","C04","AVAILABLE"},
            {"ST001","C05","AVAILABLE"},{"ST001","C06","AVAILABLE"},{"ST001","C07","AVAILABLE"},
            {"ST001","C08","AVAILABLE"},{"ST001","C09","AVAILABLE"},{"ST001","C10","AVAILABLE"},
            {"ST001","D01","AVAILABLE"},{"ST001","D02","AVAILABLE"},{"ST001","D03","AVAILABLE"},
            {"ST001","D04","AVAILABLE"},{"ST001","D05","AVAILABLE"},{"ST001","D06","AVAILABLE"},
            {"ST001","D07","AVAILABLE"},{"ST001","D08","AVAILABLE"},{"ST001","D09","AVAILABLE"},
            {"ST001","D10","AVAILABLE"},{"ST001","E01","BOOKED"},  {"ST001","E02","AVAILABLE"},
            {"ST001","E03","AVAILABLE"},{"ST001","E04","AVAILABLE"},{"ST001","E05","AVAILABLE"},
            {"ST001","E06","AVAILABLE"},{"ST001","E07","AVAILABLE"},{"ST001","E08","AVAILABLE"},
            {"ST001","E09","AVAILABLE"},{"ST001","E10","BOOKED"},

            // ST002 – SC002 (rows A-D, 8 seats)
            {"ST002","A01","AVAILABLE"},{"ST002","A02","AVAILABLE"},{"ST002","A03","AVAILABLE"},
            {"ST002","A04","AVAILABLE"},{"ST002","A05","AVAILABLE"},{"ST002","A06","AVAILABLE"},
            {"ST002","A07","AVAILABLE"},{"ST002","A08","AVAILABLE"},{"ST002","B01","AVAILABLE"},
            {"ST002","B02","AVAILABLE"},{"ST002","B03","AVAILABLE"},{"ST002","B04","AVAILABLE"},
            {"ST002","B05","AVAILABLE"},{"ST002","B06","AVAILABLE"},{"ST002","B07","AVAILABLE"},
            {"ST002","B08","AVAILABLE"},{"ST002","C01","AVAILABLE"},{"ST002","C02","AVAILABLE"},
            {"ST002","C03","AVAILABLE"},{"ST002","C04","BOOKED"},  {"ST002","C05","AVAILABLE"},
            {"ST002","C06","AVAILABLE"},{"ST002","C07","AVAILABLE"},{"ST002","C08","AVAILABLE"},
            {"ST002","D01","AVAILABLE"},{"ST002","D02","AVAILABLE"},{"ST002","D03","AVAILABLE"},
            {"ST002","D04","AVAILABLE"},{"ST002","D05","AVAILABLE"},{"ST002","D06","AVAILABLE"},
            {"ST002","D07","AVAILABLE"},{"ST002","D08","BOOKED"},

            // ST003 – SC001 (rows A-E, 10 seats)
            {"ST003","A01","BOOKED"},  {"ST003","A02","AVAILABLE"},{"ST003","A03","AVAILABLE"},
            {"ST003","A04","AVAILABLE"},{"ST003","A05","AVAILABLE"},{"ST003","A06","AVAILABLE"},
            {"ST003","A07","AVAILABLE"},{"ST003","A08","AVAILABLE"},{"ST003","A09","AVAILABLE"},
            {"ST003","A10","AVAILABLE"},{"ST003","B01","AVAILABLE"},{"ST003","B02","AVAILABLE"},
            {"ST003","B03","AVAILABLE"},{"ST003","B04","AVAILABLE"},{"ST003","B05","AVAILABLE"},
            {"ST003","B06","AVAILABLE"},{"ST003","B07","AVAILABLE"},{"ST003","B08","AVAILABLE"},
            {"ST003","B09","AVAILABLE"},{"ST003","B10","AVAILABLE"},{"ST003","C01","AVAILABLE"},
            {"ST003","C02","AVAILABLE"},{"ST003","C03","AVAILABLE"},{"ST003","C04","AVAILABLE"},
            {"ST003","C05","AVAILABLE"},{"ST003","C06","AVAILABLE"},{"ST003","C07","AVAILABLE"},
            {"ST003","C08","AVAILABLE"},{"ST003","C09","AVAILABLE"},{"ST003","C10","AVAILABLE"},
            {"ST003","D01","AVAILABLE"},{"ST003","D02","AVAILABLE"},{"ST003","D03","AVAILABLE"},
            {"ST003","D04","AVAILABLE"},{"ST003","D05","AVAILABLE"},{"ST003","D06","AVAILABLE"},
            {"ST003","D07","AVAILABLE"},{"ST003","D08","AVAILABLE"},{"ST003","D09","BOOKED"},
            {"ST003","D10","BOOKED"},  {"ST003","E01","AVAILABLE"},{"ST003","E02","AVAILABLE"},
            {"ST003","E03","AVAILABLE"},{"ST003","E04","AVAILABLE"},{"ST003","E05","AVAILABLE"},
            {"ST003","E06","AVAILABLE"},{"ST003","E07","AVAILABLE"},{"ST003","E08","AVAILABLE"},
            {"ST003","E09","AVAILABLE"},{"ST003","E10","AVAILABLE"},

            // ST004 – SC001 (rows A-E, 10 seats) – all AVAILABLE
            {"ST004","A01","AVAILABLE"},{"ST004","A02","AVAILABLE"},{"ST004","A03","AVAILABLE"},
            {"ST004","A04","AVAILABLE"},{"ST004","A05","AVAILABLE"},{"ST004","A06","AVAILABLE"},
            {"ST004","A07","AVAILABLE"},{"ST004","A08","AVAILABLE"},{"ST004","A09","AVAILABLE"},
            {"ST004","A10","AVAILABLE"},{"ST004","B01","AVAILABLE"},{"ST004","B02","AVAILABLE"},
            {"ST004","B03","AVAILABLE"},{"ST004","B04","AVAILABLE"},{"ST004","B05","AVAILABLE"},
            {"ST004","B06","AVAILABLE"},{"ST004","B07","AVAILABLE"},{"ST004","B08","AVAILABLE"},
            {"ST004","B09","AVAILABLE"},{"ST004","B10","AVAILABLE"},{"ST004","C01","AVAILABLE"},
            {"ST004","C02","AVAILABLE"},{"ST004","C03","AVAILABLE"},{"ST004","C04","AVAILABLE"},
            {"ST004","C05","AVAILABLE"},{"ST004","C06","AVAILABLE"},{"ST004","C07","AVAILABLE"},
            {"ST004","C08","AVAILABLE"},{"ST004","C09","AVAILABLE"},{"ST004","C10","AVAILABLE"},
            {"ST004","D01","AVAILABLE"},{"ST004","D02","AVAILABLE"},{"ST004","D03","AVAILABLE"},
            {"ST004","D04","AVAILABLE"},{"ST004","D05","AVAILABLE"},{"ST004","D06","AVAILABLE"},
            {"ST004","D07","AVAILABLE"},{"ST004","D08","AVAILABLE"},{"ST004","D09","AVAILABLE"},
            {"ST004","D10","AVAILABLE"},{"ST004","E01","AVAILABLE"},{"ST004","E02","AVAILABLE"},
            {"ST004","E03","AVAILABLE"},{"ST004","E04","AVAILABLE"},{"ST004","E05","AVAILABLE"},
            {"ST004","E06","AVAILABLE"},{"ST004","E07","AVAILABLE"},{"ST004","E08","AVAILABLE"},
            {"ST004","E09","AVAILABLE"},{"ST004","E10","AVAILABLE"},

            // ST005 – SC002 (rows A-D, 8 seats)
            {"ST005","A01","BOOKED"},  {"ST005","A02","BOOKED"},  {"ST005","A03","AVAILABLE"},
            {"ST005","A04","AVAILABLE"},{"ST005","A05","AVAILABLE"},{"ST005","A06","AVAILABLE"},
            {"ST005","A07","AVAILABLE"},{"ST005","A08","AVAILABLE"},{"ST005","B01","AVAILABLE"},
            {"ST005","B02","AVAILABLE"},{"ST005","B03","AVAILABLE"},{"ST005","B04","AVAILABLE"},
            {"ST005","B05","AVAILABLE"},{"ST005","B06","AVAILABLE"},{"ST005","B07","AVAILABLE"},
            {"ST005","B08","AVAILABLE"},{"ST005","C01","AVAILABLE"},{"ST005","C02","AVAILABLE"},
            {"ST005","C03","AVAILABLE"},{"ST005","C04","AVAILABLE"},{"ST005","C05","AVAILABLE"},
            {"ST005","C06","AVAILABLE"},{"ST005","C07","AVAILABLE"},{"ST005","C08","AVAILABLE"},
            {"ST005","D01","AVAILABLE"},{"ST005","D02","AVAILABLE"},{"ST005","D03","AVAILABLE"},
            {"ST005","D04","AVAILABLE"},{"ST005","D05","AVAILABLE"},{"ST005","D06","AVAILABLE"},
            {"ST005","D07","AVAILABLE"},{"ST005","D08","AVAILABLE"},
        };

        int n = 0;
        try (PreparedStatement ps   = conn.prepareStatement(sql);
             PreparedStatement chk  = conn.prepareStatement(checkSql)) {
            for (String[] row : showSeats) {
                chk.setString(1, row[1]); // seatId
                chk.setString(2, row[0]); // showtimeId
                try (ResultSet rs = chk.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) == 0) {
                        ps.setString(1, row[1]); // seatId
                        ps.setString(2, row[0]); // showtimeId
                        ps.setString(3, row[2]); // status
                        ps.executeUpdate();
                        n++;
                    }
                }
            }
        }
        System.out.println("   " + n + " show seat(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Bookings  (from bookings.txt)
    // Format: bookingCode|showtimeId|totalPrice|seatId,type,price[;...]
    //
    // BK-D911CE85|ST001|25.00|E10,ADULT,25.00
    // BK-0FA63A97|ST003|15.40|A01,SENIOR,15.40
    // BK-6B1D2AEE|ST001|18.75|E01,STUDENT,18.75
    // BK-0E8CBFCB|ST003|38.50|D10,STUDENT,16.50;D09,ADULT,22.00
    // BK-7F94D2FB|ST005|37.40|A01,ADULT,22.00;A02,SENIOR,15.40
    // BK-F114F166|ST001|40.00|A02,CHILD,15.00;A03,ADULT,25.00
    // BK-9E7E52CA|ST002|40.00|C04,ADULT,25.00;D08,CHILD,15.00
    //
    // All bookings are attributed to the "guest" account.
    // -----------------------------------------------------------------------

    private static void seedBookings(Connection conn) throws SQLException {
        System.out.println("\n[8/8] Seeding bookings...");

        String bookingSql =
            "INSERT INTO Booking (bookingCode, bookingDate, totalPrice, username) VALUES (?,?,?,?)";
        String itemSql =
            "INSERT INTO BookingItem (bookingCode, seatId, itemPrice, attendeeType) VALUES (?,?,?,?)";

        // {bookingCode, showtimeId, totalPrice, items: "seatId,type,price;..."}
        // Date is set to 2026-08-01 as a plausible booking-made date before showtimes
        Object[][] bookings = {
            {"BK-D911CE85", "ST001", 25.00, "E10,ADULT,25.00"},
            {"BK-0FA63A97", "ST003", 15.40, "A01,SENIOR,15.40"},
            {"BK-6B1D2AEE", "ST001", 18.75, "E01,STUDENT,18.75"},
            {"BK-0E8CBFCB", "ST003", 38.50, "D10,STUDENT,16.50;D09,ADULT,22.00"},
            {"BK-7F94D2FB", "ST005", 37.40, "A01,ADULT,22.00;A02,SENIOR,15.40"},
            {"BK-F114F166", "ST001", 40.00, "A02,CHILD,15.00;A03,ADULT,25.00"},
            {"BK-9E7E52CA", "ST002", 40.00, "C04,ADULT,25.00;D08,CHILD,15.00"},
        };

        java.sql.Date bookingDate = java.sql.Date.valueOf("2026-08-01");
        int n = 0;

        try (PreparedStatement bps = conn.prepareStatement(bookingSql);
             PreparedStatement ips = conn.prepareStatement(itemSql)) {

            for (Object[] b : bookings) {
                String code      = (String) b[0];
                double total     = (double) b[2];
                String itemsStr  = (String) b[3];

                if (!exists(conn, "Booking", "bookingCode", code)) {
                    // Insert booking header
                    bps.setString(1, code);
                    bps.setDate(2, bookingDate);
                    bps.setDouble(3, total);
                    bps.setString(4, "guest");
                    bps.executeUpdate();

                    // Insert each item
                    for (String itemStr : itemsStr.split(";")) {
                        String[] fields = itemStr.split(",");
                        String seatId       = fields[0];
                        String attendeeType = fields[1];
                        double itemPrice    = Double.parseDouble(fields[2]);

                        ips.setString(1, code);
                        ips.setString(2, seatId);
                        ips.setDouble(3, itemPrice);
                        ips.setString(4, attendeeType);
                        ips.executeUpdate();
                    }

                    System.out.println("   + " + code);
                    n++;
                } else {
                    System.out.println("   ~ already exists: " + code);
                }
            }
        }
        System.out.println("   " + n + " booking(s) inserted.");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static boolean exists(Connection conn, String table, String column, String value)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static void createIfAbsent(Statement st, String name, String ddl)
            throws SQLException {
        try {
            st.execute(ddl);
            System.out.println("   + created: " + name);
        } catch (SQLException e) {
            if ("X0Y32".equals(e.getSQLState())) {
                System.out.println("   ~ already exists: " + name);
            } else {
                throw e;
            }
        }
    }

    private static void shutdownDerby() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            if ("XJ015".equals(e.getSQLState())) {
                System.out.println("\nDerby shut down cleanly.");
            }
        }
    }
}
