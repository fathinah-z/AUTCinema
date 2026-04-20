package cinemaapp.repository;

import cinemaapp.model.ShowSeat;
import cinemaapp.model.SeatStatus;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileShowSeatRepository implements ShowSeatRepository {

    // Key: "showtimeId:seatId" -> SeatStatus
    private final Map<String, SeatStatus> seatStatusMap = new HashMap<>();
    private final String filepath = "src/showseats.txt";

    public FileShowSeatRepository() {
        load();
    }

    @Override
    public HashMap<String, SeatStatus> findByShowtimeId(String showtimeId) {
        HashMap<String, SeatStatus> result = new HashMap<>();
        String prefix = showtimeId + ":";
        for (Map.Entry<String, SeatStatus> entry : seatStatusMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String seatId = entry.getKey().substring(prefix.length());
                result.put(seatId, entry.getValue());
            }
        }
        return result;
    }

    @Override
    public void updateSeatStatus(String showtimeId, String seatId, SeatStatus status) {
        String key = showtimeId + ":" + seatId;
        seatStatusMap.put(key, status);
    }

    private ShowSeat parseShowSeat(String line) {
        String[] parts = line.split("\\|");

        String showtimeId = parts[0];
        String seatId = parts[1];
        SeatStatus status = SeatStatus.valueOf(parts[2]);

        return new ShowSeat(showtimeId, seatId, status);
    }

    private void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;

            while ((line = br.readLine()) != null) {
                ShowSeat ss = parseShowSeat(line);
                String key = ss.getShowtimeId() + ":" + ss.getSeatId();
                seatStatusMap.put(key, ss.getSeatStatus());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load showseats file", e);
        }
    }
}
