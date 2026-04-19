package cinemaapp.filter;

import cinemaapp.model.Seat;

public class RowRangeFilter implements SeatFilter {
    private char minRow;
    private char maxRow;

    public RowRangeFilter(char minRow, char maxRow) {
        this.minRow = minRow;
        this.maxRow = maxRow;
    }

    public char getMinRow() { return minRow; }
    public void setMinRow(char minRow) { this.minRow = minRow; }

    public char getMaxRow() { return maxRow; }
    public void setMaxRow(char maxRow) { this.maxRow = maxRow; }

    @Override
    public boolean matches(Seat seat) {
        return seat.getRow() >= minRow && seat.getRow() <= maxRow;
    }
}
