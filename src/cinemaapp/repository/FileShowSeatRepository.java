package cinemaapp.repository;

import cinemaapp.model.SeatStatus;
import java.util.HashMap;
import java.util.Map;

public class FileShowSeatRepository implements ShowSeatRepository {

    // Key: "showtimeId:seatId" -> SeatStatus
    private final Map<String, SeatStatus> seatStatusMap = new HashMap<>();

    public FileShowSeatRepository() {
        // Seed initial seat statuses for showtimes
        String[] showtimeIds = {"ST001", "ST002", "ST003", "ST004", "ST005"};
        char[] rows = {'A', 'B', 'C', 'D', 'E'};
        int seatsPerRow = 8;

        for (String stId : showtimeIds) {
            for (char row : rows) {
                for (int i = 1; i <= seatsPerRow; i++) {
                    String seatId = row + String.format("%02d", i);
                    seatStatusMap.put(stId + ":" + seatId, SeatStatus.AVAILABLE);
                }
            }
        }
        // Mark a few seats as booked for realism
        seatStatusMap.put("ST001:A01", SeatStatus.BOOKED);
        seatStatusMap.put("ST001:A02", SeatStatus.BOOKED);
        seatStatusMap.put("ST002:B03", SeatStatus.RESERVED);
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
}
