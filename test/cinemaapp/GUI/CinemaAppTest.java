package cinemaapp.GUI;

import cinemaapp.model.*;
import cinemaapp.service.PricingService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDateTime;
import cinemaapp.service.CancelBookingService;
import cinemaapp.model.Showtime;
import cinemaapp.repository.ShowtimeRepository;
import java.util.List;
import java.util.ArrayList;

public class CinemaAppTest {

    public CinemaAppTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // attendee type and price modifiers
    @Test
    public void testChildPriceModifier() {
        assertEquals(0.6, AttendeeType.CHILD.getPriceModifier(), 0.001);
    }

    @Test
    public void testAdultPriceModifier() {
        assertEquals(1.0, AttendeeType.ADULT.getPriceModifier(), 0.001);
    }

    @Test
    public void testStudentPriceModifier() {
        assertEquals(0.75, AttendeeType.STUDENT.getPriceModifier(), 0.001);
    }

    @Test
    public void testSeniorPriceModifier() {
        assertEquals(0.7, AttendeeType.SENIOR.getPriceModifier(), 0.001);
    }

    // r18
    @Test
    public void testMovieRatingR18Exists() {
        MovieRating rating = MovieRating.R18;
        assertNotNull(rating);
        assertEquals("R18", rating.toString());
    }

    // test BookingItem stores values correctly
    @Test
    public void testBookingItemPrice() {
        BookingItem item = new BookingItem("A1", AttendeeType.ADULT, 15.00);
        assertEquals(15.00, item.getItemPrice(), 0.001);
        assertEquals("A1", item.getSeatId());
        assertEquals(AttendeeType.ADULT, item.getAttendeeType());
    }

    // test that a child attendee type is correctly identified
    @Test
    public void testChildAttendeeTypeIsChild() {
        AttendeeType type = AttendeeType.CHILD;
        assertEquals(AttendeeType.CHILD, type);
        assertNotEquals(AttendeeType.ADULT, type);
    }

    // test Movie model stores fields correctly
    @Test
    public void testMovieGetTitle() {
        Movie m = new Movie("M1", "Test Movie", MovieRating.PG, "A description", 120);
        assertEquals("Test Movie", m.getTitle());
        assertEquals(MovieRating.PG, m.getRating());
        assertEquals(120, m.getRuntime());
    }

    // test that R18 movie rating is detected correctly
    @Test
    public void testMovieIsR18() {
        Movie m = new Movie("M2", "Restricted Film", MovieRating.R18, "Adults only", 90);
        assertEquals(MovieRating.R18, m.getRating());
    }

    @Test
    public void testRefundEligibleWhenShowtimeMoreThan5DaysAway() {
        // Showtime is 10 days from now — should be eligible
        Showtime showtime = new Showtime("ST001", "M1", "SC1",
                LocalDateTime.now().plusDays(10), 20.00);

        ShowtimeRepository mockShowtimeRepo = new ShowtimeRepository() {
            @Override
            public Showtime findById(String id) {
                return showtime;
            }

            @Override
            public List<Showtime> findByMovieId(String movieId) {
                return new ArrayList<>();
            }
        };

        CancelBookingService service = new CancelBookingService(
                null, null, mockShowtimeRepo);

        Booking booking = new Booking("BK-TEST001", "ST001");
        assertTrue("Should be eligible when showtime is 10 days away",
                service.isRefundEligible(booking, LocalDateTime.now()));
    }

    @Test
    public void testRefundNotEligibleWhenShowtimeWithin5Days() {
        // Showtime is 2 days from now — should NOT be eligible
        Showtime showtime = new Showtime("ST002", "M1", "SC1",
                LocalDateTime.now().plusDays(2), 20.00);

        ShowtimeRepository mockShowtimeRepo = new ShowtimeRepository() {
            @Override
            public Showtime findById(String id) {
                return showtime;
            }

            @Override
            public List<Showtime> findByMovieId(String movieId) {
                return new ArrayList<>();
            }
        };

        CancelBookingService service = new CancelBookingService(
                null, null, mockShowtimeRepo);

        Booking booking = new Booking("BK-TEST002", "ST002");
        assertFalse("Should not be eligible when showtime is only 2 days away",
                service.isRefundEligible(booking, LocalDateTime.now()));
    }

    @Test
    public void testRefundNotEligibleForNullBooking() {
        CancelBookingService service = new CancelBookingService(null, null, null);
        assertFalse("Null booking should return false",
                service.isRefundEligible(null, LocalDateTime.now()));
    }

    @Test
    public void testRefundNotEligibleWhenShowtimeAlreadyPassed() {
        // Showtime was yesterday — definitely not eligible
        Showtime showtime = new Showtime("ST003", "M1", "SC1",
                LocalDateTime.now().minusDays(1), 20.00);

        ShowtimeRepository mockShowtimeRepo = new ShowtimeRepository() {
            @Override
            public Showtime findById(String id) {
                return showtime;
            }

            @Override
            public List<Showtime> findByMovieId(String movieId) {
                return new ArrayList<>();
            }
        };

        CancelBookingService service = new CancelBookingService(
                null, null, mockShowtimeRepo);

        Booking booking = new Booking("BK-TEST003", "ST003");
        assertFalse("Should not be eligible when showtime has already passed",
                service.isRefundEligible(booking, LocalDateTime.now()));
    }
}
