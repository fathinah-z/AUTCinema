package cinemaapp.model;

import java.util.List;
import java.util.ArrayList;

public class Screen {

    private final String screenId;
    private final char firstRow;
    private final char lastRow;
    private final int seatsPerRow;
    private final List<Seat> seatingLayout;

    public Screen(String screenId, char firstRow, char lastRow, int seatsPerRow, List<Seat> seatingLayout) {
        this.screenId = screenId;
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.seatsPerRow = seatsPerRow;
        this.seatingLayout = seatingLayout;
    }

    public String getScreenId() {
        return screenId;
    }

    public char getFirstRow() {
        return firstRow;
    }

    public char getLastRow() {
        return lastRow;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }

    public List<Seat> getSeatingLayout() {
        return seatingLayout;
    }
}
