package cinemaapp.filter;

import cinemaapp.model.Seat;

public abstract class SeatFilter {
    public abstract boolean matches(Seat seat);
}
