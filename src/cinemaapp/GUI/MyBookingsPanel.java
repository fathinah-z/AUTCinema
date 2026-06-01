package cinemaapp.GUI;

import cinemaapp.dao.BookingDAO;
import cinemaapp.dao.MovieDAO;
import cinemaapp.dao.ShowtimeDAO;
import cinemaapp.model.*;
import cinemaapp.util.TicketPDFGenerator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * My Bookings tab.
 *
 * BUGS FIXED from previous version:
 *
 *   BUG 1 — Wrong method name called from MainFrame:
 *     Was:  myBookings::refresh
 *     Fix:  method is named loadBookings(), not refresh().
 *     The method is public so MainFrame can pass it as a Runnable callback.
 *
 *   BUG 2 — BookingItem.getShowtimeId() does not exist:
 *     BookingItem only has getSeatId(), getAttendeeType(), getItemPrice().
 *     The showtimeId lives on Booking itself (booking.getShowtimeId()).
 *     Was:  booking.getBookingItems().get(0).getShowtimeId()
 *     Fix:  booking.getShowtimeId()
 *
 *   BUG 3 — MovieDAO.findById() and ShowtimeDAO.findById() declare
 *     throws SQLException but the catch in downloadPDF() only caught
 *     a generic Exception after the try block — meaning SQL exceptions
 *     from the DAO calls were not caught at the right level.
 *     Fix:  wrap each DAO call in its own try-catch inside the resolve block.
 */
public class MyBookingsPanel extends JPanel {

    private final BookingDAO  bookingDAO;
    private final MovieDAO    movieDAO;
    private final ShowtimeDAO showtimeDAO;
    private final String      username;

    private DefaultTableModel tableModel;
    private JTable            bookingsTable;
    private JTextArea         detailArea;

    // Parallel list — same order as table rows
    private final List<Booking> loadedBookings = new ArrayList<>();

    public MyBookingsPanel(BookingDAO bookingDAO,
                           MovieDAO movieDAO,
                           ShowtimeDAO showtimeDAO,
                           String username) {
        this.bookingDAO  = bookingDAO;
        this.movieDAO    = movieDAO;
        this.showtimeDAO = showtimeDAO;
        this.username    = username;
        buildUI();
        loadBookings();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(12, 14, 12, 14));

        // Header row
        JPanel header = new JPanel(new BorderLayout());
        JLabel heading = new JLabel("Booking History — " + username);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.addActionListener(e -> loadBookings());
        header.add(heading,    BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Bookings table
        String[] cols = {"Booking Code", "Date Booked", "Showtime", "Seats", "Total Paid"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        bookingsTable = new JTable(tableModel);
        bookingsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookingsTable.setRowHeight(22);
        bookingsTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        bookingsTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        bookingsTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        bookingsTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        bookingsTable.getColumnModel().getColumn(4).setPreferredWidth(90);
        bookingsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedDetails();
        });

        // Detail area under the table
        detailArea = new JTextArea(7, 40);
        detailArea.setEditable(false);
        detailArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        detailArea.setBorder(new EmptyBorder(6, 8, 6, 8));

        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(new TitledBorder("Selected Booking Details"));
        detailPanel.add(new JScrollPane(detailArea));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(bookingsTable), detailPanel);
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        // PDF button
        JButton pdfBtn = new JButton("⬇  Download Ticket");
        pdfBtn.setFont(pdfBtn.getFont().deriveFont(Font.BOLD, 13f));
        pdfBtn.setBackground(new Color(60, 110, 180));
        pdfBtn.setForeground(Color.WHITE);
        pdfBtn.setFocusPainted(false);
        pdfBtn.addActionListener(e -> downloadTicket());
        add(pdfBtn, BorderLayout.SOUTH);
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    /**
     * Public so MainFrame can pass this as a Runnable callback:
     *   myBookings::loadBookings
     * Called after a booking is made or cancelled.
     */
    public void loadBookings() {
        tableModel.setRowCount(0);
        loadedBookings.clear();
        detailArea.setText("");

        try {
            List<Booking> bookings = bookingDAO.findByUsername(username);
            for (Booking b : bookings) {
                loadedBookings.add(b);

                // Resolve showtime date for the table (non-fatal if it fails)
                String showtimeDisplay = b.getShowtimeId();
                try {
                    Showtime st = showtimeDAO.findById(b.getShowtimeId());
                    if (st != null) {
                        showtimeDisplay = st.getDateTime().toString();
                    }
                } catch (SQLException ignored) {}

                tableModel.addRow(new Object[]{
                    b.getBookingCode(),
                    b.getBookingDate(),
                    showtimeDisplay,
                    b.getBookingItems().size() + " seat(s)",
                    String.format("$%.2f", b.getTotalPrice())
                });
            }
            if (bookings.isEmpty()) {
                detailArea.setText("No bookings found for this account.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not load bookings:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    private void showSelectedDetails() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0 || row >= loadedBookings.size()) return;

        Booking b = loadedBookings.get(row);
        StringBuilder sb = new StringBuilder();
        sb.append("Booking Code : ").append(b.getBookingCode()).append("\n");
        sb.append("Booked On    : ").append(b.getBookingDate()).append("\n");

        // FIX BUG 2: showtimeId is on Booking, not BookingItem
        try {
            Showtime st = showtimeDAO.findById(b.getShowtimeId());
            if (st != null) {
                sb.append("Showtime     : ").append(st.getDateTime())
                  .append("  |  Screen ").append(st.getScreenId()).append("\n");
            }
        } catch (SQLException ignored) {}

        sb.append("\nSeats:\n");
        for (BookingItem item : b.getBookingItems()) {
            sb.append("  Seat ").append(item.getSeatId())
              .append("  [").append(item.getAttendeeType()).append("]")
              .append("  $").append(String.format("%.2f", item.getItemPrice()))
              .append("\n");
        }
        sb.append(String.format("\nTotal Paid   : $%.2f", b.getTotalPrice()));
        detailArea.setText(sb.toString());
    }

    private void downloadTicket() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select a booking from the table first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Booking booking = loadedBookings.get(row);

        // FIX BUG 2: get showtimeId from Booking, not from BookingItem
        String movieTitle   = "AUT Cinema";
        String showtimeInfo = "";

        try {
            // showtimeId is a field on Booking itself
            Showtime st = showtimeDAO.findById(booking.getShowtimeId());
            if (st != null) {
                showtimeInfo = st.getDateTime() + "  |  Screen " + st.getScreenId();
                // FIX BUG 3: MovieDAO.findById() throws SQLException — catch it here
                try {
                    Movie m = movieDAO.findById(st.getMovieId());
                    if (m != null) movieTitle = m.getTitle();
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ignored) {}
        // Non-fatal — ticket still generates with placeholder text if lookups fail

        // Ask where to save
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Ticket");
        chooser.setSelectedFile(new File(booking.getBookingCode() + "_ticket.html"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();
        if (!path.endsWith(".html") && !path.endsWith(".pdf")) path += ".html";

        try {
            new TicketPDFGenerator().generate(booking, movieTitle, showtimeInfo, path);
            JOptionPane.showMessageDialog(this,
                    "Ticket saved:\n" + path,
                    "Ticket Ready", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to generate ticket:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}