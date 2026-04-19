package cinemaapp.filter;

import cinemaapp.model.Seat;

public interface SeatFilter {
    boolean matches(Seat seat);
}
