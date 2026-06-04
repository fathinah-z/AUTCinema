package cinemaapp.GUI;

import cinemaapp.dao.MovieDAO;
import cinemaapp.dao.ShowtimeDAO;
import cinemaapp.model.Booking;
import cinemaapp.model.BookingItem;
import cinemaapp.model.Movie;
import cinemaapp.model.Showtime;
import cinemaapp.repository.DbBookingRepository;
import cinemaapp.service.CancelBookingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CancelPanel extends JPanel {

    private final CancelBookingService cancelService;
    private final DbBookingRepository bookingRepo;
    private final MovieDAO movieDAO;
    private final ShowtimeDAO showtimeDAO;

    private JTextField codeField;
    private JTextArea detailsArea;
    private JLabel refundLabel;
    private JButton confirmCancelBtn;

    private Booking foundBooking;

    public CancelPanel(CancelBookingService cancelService,
            DbBookingRepository bookingRepo,
            MovieDAO movieDAO,
            ShowtimeDAO showtimeDAO) {
        this.cancelService = cancelService;
        this.bookingRepo = bookingRepo;
        this.movieDAO = movieDAO;
        this.showtimeDAO = showtimeDAO;
        buildUI();
    }

    private void buildUI() {
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.disabledForeground", Color.GRAY);
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 30, 20, 30));

        // Top: code entry row
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topRow.add(new JLabel("Booking Code:"));
        codeField = new JTextField(20);
        JButton lookupBtn = new JButton("Look Up");
        lookupBtn.addActionListener(e -> handleLookup());
        topRow.add(codeField);
        topRow.add(lookupBtn);

        // Centre: booking details
        JPanel centrePanel = new JPanel(new BorderLayout(0, 8));
        centrePanel.setBorder(new TitledBorder("Booking Details"));

        detailsArea = new JTextArea(10, 40);
        detailsArea.setEditable(false);
        detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        detailsArea.setText("Enter a booking code above and click Look Up.");

        refundLabel = new JLabel(" ");
        refundLabel.setFont(refundLabel.getFont().deriveFont(Font.BOLD, 13f));
        refundLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        centrePanel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
        centrePanel.add(refundLabel, BorderLayout.SOUTH);

        //cancel button
        confirmCancelBtn = new JButton("<html><span style='color:black;font-weight:bold'>✕  Confirm Cancellation</span></html>");
        confirmCancelBtn.setEnabled(false);
        confirmCancelBtn.setBackground(new Color(220, 220, 220));
        confirmCancelBtn.setOpaque(true);
        confirmCancelBtn.addActionListener(e -> handleCancel());

        add(topRow, BorderLayout.NORTH);
        add(centrePanel, BorderLayout.CENTER);
        add(confirmCancelBtn, BorderLayout.SOUTH);
    }

    private void handleLookup() {
        String code = codeField.getText().trim().toUpperCase();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a booking code.");
            return;
        }

        foundBooking = bookingRepo.findByBookingCode(code);

        if (foundBooking == null) {
            detailsArea.setText("No booking found with code: " + code);
            refundLabel.setText(" ");
            confirmCancelBtn.setEnabled(false);
            return;
        }

        // Resolve movie title and showtime info
        String movieTitle = "Unknown";
        String showtimeDisplay = "";
        try {
            Showtime st = showtimeDAO.findById(foundBooking.getShowtimeId());
            if (st != null) {
                showtimeDisplay = st.getDateTime() + "  |  Screen " + st.getScreenId();
                try {
                    Movie m = movieDAO.findById(st.getMovieId());
                    if (m != null) {
                        movieTitle = m.getTitle();
                    }
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException ignored) {
        }

        // Show booking details with movie name
        StringBuilder sb = new StringBuilder();
        sb.append("Booking Code  : ").append(foundBooking.getBookingCode()).append("\n");
        sb.append("Movie         : ").append(movieTitle).append("\n");
        if (!showtimeDisplay.isEmpty()) {
            sb.append("Showtime      : ").append(showtimeDisplay).append("\n");
        }
        sb.append("Booked On     : ").append(foundBooking.getBookingDate()).append("\n\n");
        sb.append("Seats:\n");
        for (BookingItem item : foundBooking.getBookingItems()) {
            sb.append("  Seat ").append(item.getSeatId())
                    .append("  [").append(item.getAttendeeType()).append("]")
                    .append("  $").append(String.format("%.2f", item.getItemPrice()))
                    .append("\n");
        }
        sb.append(String.format("\nTotal Paid    : $%.2f", foundBooking.getTotalPrice()));
        detailsArea.setText(sb.toString());

        // Refund eligibility
        boolean eligible = cancelService.isRefundEligible(foundBooking, LocalDateTime.now());
        if (eligible) {
            refundLabel.setForeground(new Color(40, 160, 80));
            refundLabel.setText("✔  Eligible for full refund of $"
                    + String.format("%.2f", foundBooking.getTotalPrice()));
        } else {
            refundLabel.setForeground(new Color(200, 60, 60));
            refundLabel.setText("✕  Outside refund window — no refund will be issued.");
        }

        confirmCancelBtn.setEnabled(true);
    }

    private void handleCancel() {
        if (foundBooking == null) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Cancel booking " + foundBooking.getBookingCode() + "?\nThis cannot be undone.",
                "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        boolean eligible = cancelService.isRefundEligible(foundBooking, LocalDateTime.now());
        boolean ok = cancelService.cancelBooking(foundBooking);

        if (ok) {
            String refundMsg = eligible
                    ? String.format("Refund of $%.2f processed.", foundBooking.getTotalPrice())
                    : "No refund issued (outside window).";
            JOptionPane.showMessageDialog(this,
                    "Booking " + foundBooking.getBookingCode() + " cancelled.\n" + refundMsg,
                    "Cancelled", JOptionPane.INFORMATION_MESSAGE);

            codeField.setText("");
            detailsArea.setText("Enter a booking code above and click Look Up.");
            refundLabel.setText(" ");
            confirmCancelBtn.setEnabled(false);
            foundBooking = null;
        } else {
            JOptionPane.showMessageDialog(this,
                    "Cancellation failed. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
