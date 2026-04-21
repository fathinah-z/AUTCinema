package cinemaapp.repository;

import cinemaapp.model.ShowSeat;
import cinemaapp.model.SeatStatus;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileShowSeatRepository implements ShowSeatRepository {

    // Key: "showtimeId:seatId" -> SeatStatus
    private final Map<String, SeatStatus> showSeats = new LinkedHashMap<>();
    private final String filepath = "src/showseats.txt";

    public FileShowSeatRepository() {
        load();
    }

    @Override
    public Map<String, SeatStatus> findByShowtimeId(String showtimeId) {
        Map<String, SeatStatus> result = new LinkedHashMap<>();
        String prefix = showtimeId + ":";
        for (Map.Entry<String, SeatStatus> entry : showSeats.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String seatId = entry.getKey().substring(prefix.length());
                result.put(seatId, entry.getValue());
            }
        }
        return result;
    }

    @Override
    public void resetAllReservedSeats() {
        for (Map.Entry<String, SeatStatus> entry : showSeats.entrySet()) {
            if (entry.getValue() == SeatStatus.RESERVED) {
                entry.setValue(SeatStatus.AVAILABLE);
            }
        }
        saveToFile();
    }

    @Override
    public void updateSeatStatus(String showtimeId, String seatId, SeatStatus status) {
        String key = showtimeId + ":" + seatId;
        showSeats.put(key, status);
        saveToFile();
    }

    @Override
    public synchronized boolean tryReserveSeat(String showtimeId, String seatId) {
        Map<String, SeatStatus> map = findByShowtimeId(showtimeId);
        SeatStatus current = map.get(seatId);

        if (current == SeatStatus.AVAILABLE) {
            map.put(seatId, SeatStatus.RESERVED);
            saveToFile();
            return true;
        }

        return false;
    }

    @Override
    public synchronized boolean tryBookSeat(String showtimeId, String seatId) {
        Map<String, SeatStatus> map = findByShowtimeId(showtimeId);
        SeatStatus current = map.get(seatId);

        if (current == SeatStatus.RESERVED) {
            map.put(seatId, SeatStatus.BOOKED);
            saveToFile();
            return true;
        }

        return false;
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
                if (line.isBlank()) {
                    continue;
                }
                ShowSeat ss = parseShowSeat(line);
                String key = ss.getShowtimeId() + ":" + ss.getSeatId();
                showSeats.put(key, ss.getSeatStatus());
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to load showseats file", e);
        }
    }

    private void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath))) {
            for (Map.Entry<String, SeatStatus> entry : showSeats.entrySet()) {
                String key = entry.getKey(); // "showtimeId:seatId"
                SeatStatus status = entry.getValue();

                String[] parts = key.split(":");
                String showtimeId = parts[0];
                String seatId = parts[1];

                bw.write(showtimeId + "|" + seatId + "|" + status);
                bw.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save showseats file", e);
        }
    }

}
