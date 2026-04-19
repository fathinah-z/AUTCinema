package cinemaapp.repository;

import cinemaapp.model.Screen;
import cinemaapp.model.Seat;
import java.util.ArrayList;
import java.util.List;

public class FileScreenRepository implements ScreenRepository {

    private final List<Screen> screens = new ArrayList<>();

    public FileScreenRepository() {
        // Seed screens with seats
        screens.add(buildScreen("SC001", 'A', 'E', 8));
        screens.add(buildScreen("SC002", 'A', 'D', 10));
        screens.add(buildScreen("SC003", 'A', 'F', 6));
    }

    private Screen buildScreen(String screenId, char firstRow, char lastRow, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();
        for (char row = firstRow; row <= lastRow; row++) {
            for (int i = 1; i <= seatsPerRow; i++) {
                String seatId = row + String.format("%02d", i);
                boolean nearAisle = (i == 1 || i == seatsPerRow);
                boolean accessible = (row == lastRow && (i == 1 || i == 2));
                seats.add(new Seat(seatId, row, nearAisle, accessible));
            }
        }
        return new Screen(screenId, seats);
    }

    @Override
    public Screen findById(String screenId) {
        return screens.stream()
                .filter(s -> s.getScreenId().equals(screenId))
                .findFirst()
                .orElse(null);
    }
}
