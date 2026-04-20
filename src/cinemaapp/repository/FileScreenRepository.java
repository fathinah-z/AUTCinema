package cinemaapp.repository;

import cinemaapp.model.Movie;
import cinemaapp.model.Screen;
import cinemaapp.model.Seat;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileScreenRepository implements ScreenRepository {

    private final List<Screen> screens = new ArrayList<>();
    private final String filepath = "src/screens.txt";
    
    public FileScreenRepository() {
        load();
    }

    private void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = br.readLine()) != null) {
                Screen screen = parseScreen(line);
                screens.add(screen);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read movies file", e);
        }
    }

    private Screen buildScreen(String screenId, char firstRow, char lastRow, int seatsPerRow) {
        List<Seat> seats = new ArrayList<>();
        int middle = seatsPerRow / 2;
        for (char row = firstRow; row <= lastRow; row++) {
            for (int i = 1; i <= seatsPerRow; i++) {
                String seatId = row + String.format("%02d", i);
                boolean nearAisle = (i == 1 || i == seatsPerRow);
                boolean accessible = (row == lastRow && (i == 1 || i == 2 || i == middle || i == middle+1));
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

    private Screen parseScreen(String line) {
        String[] parts = line.split("\\|");

        String screenId = parts[0];
        char firstRow = parts[1].charAt(0);
        char lastRow = parts[2].charAt(0);
        int seatsPerRow = Integer.parseInt(parts[3]);

        return buildScreen(screenId, firstRow, lastRow, seatsPerRow);
    }
}
