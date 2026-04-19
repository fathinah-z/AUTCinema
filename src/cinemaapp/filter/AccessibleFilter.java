package cinemaapp.filter;

import cinemaapp.model.Seat;

public class AccessibleFilter implements SeatFilter {
    private boolean aisleOnly;

    public AccessibleFilter(boolean aisleOnly) {
        this.aisleOnly = aisleOnly;
    }

    public boolean isAisleOnly() { return aisleOnly; }
    public void setAisleOnly(boolean aisleOnly) { this.aisleOnly = aisleOnly; }

    @Override
    public boolean matches(Seat seat) {
        return !aisleOnly || seat.isAccessible();
    }
}
