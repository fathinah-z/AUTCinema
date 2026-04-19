package cinemaapp.model;

import java.util.List;
import java.util.ArrayList;

public class Screen {
    private String screenId;
    private List<Seat> seatingLayout;

    public Screen(String screenId) {
        this.screenId = screenId;
        this.seatingLayout = new ArrayList<>();
    }

    public Screen(String screenId, List<Seat> seatingLayout) {
        this.screenId = screenId;
        this.seatingLayout = seatingLayout;
    }

    public String getScreenId() { return screenId; }
    public void setScreenId(String screenId) { this.screenId = screenId; }

    public List<Seat> getSeatingLayout() { return seatingLayout; }
    public void setSeatingLayout(List<Seat> seatingLayout) { this.seatingLayout = seatingLayout; }

    @Override
    public String toString() {
        return String.format("Screen[%s | Seats:%d]", screenId, seatingLayout.size());
    }
}
