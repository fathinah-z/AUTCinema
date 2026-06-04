package cinemaapp.GUI;

import cinemaapp.model.*;
import cinemaapp.service.PricingService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
}