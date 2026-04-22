package cinemaapp.model;

import java.util.List;

public class Screen {

    private final String screenId;
    private char firstRow;
    private char lastRow;
    private int seatsPerRow;
    private List<Seat> seatingLayout;

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
    
    public void setFirstRow(char firstRow) {
        this.firstRow = firstRow;
    }

    public char getLastRow() {
        return lastRow;
    }
    
    public void setLastRow(char lastRow) {
        this.lastRow = lastRow;
    }

    public int getSeatsPerRow() {
        return seatsPerRow;
    }
    
    public void setSeatsPerRow(int seatsPerRow) {
        this.seatsPerRow = seatsPerRow;
    }

    public List<Seat> getSeatingLayout() {
        return seatingLayout;
    }
    
    public void setSeatingLayout(List<Seat> seatingLayout) {
        this.seatingLayout = seatingLayout;
    }
}
