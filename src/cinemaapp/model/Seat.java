package cinemaapp.model;

public class Seat {

    private String seatId;
    private char row;
    private int number;
    private boolean nearAisle;
    private boolean isAccessible;

    public Seat(String seatId, char row, int number, boolean nearAisle, boolean isAccessible) {
        this.seatId = seatId;
        this.row = row;
        this.number = number;
        this.nearAisle = nearAisle;
        this.isAccessible = isAccessible;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public char getRow() {
        return row;
    }

    public void setRow(char row) {
        this.row = row;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isNearAisle() {
        return nearAisle;
    }

    public void setNearAisle(boolean nearAisle) {
        this.nearAisle = nearAisle;
    }

    public boolean isAccessible() {
        return isAccessible;
    }

    public void setAccessible(boolean accessible) {
        isAccessible = accessible;
    }

    @Override
    public String toString() {
        return String.format("Seat[%s | Row:%c | Aisle:%b | Accessible:%b]",
                seatId, row, nearAisle, isAccessible);
    }
}
