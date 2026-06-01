package cinemaapp.GUI;

import cinemaapp.dto.MovieDetails;
import cinemaapp.dto.ShowInfo;
import cinemaapp.filter.*;
import cinemaapp.model.*;
import cinemaapp.service.BrowsingService;
import cinemaapp.service.MakeBookingService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Browse & Book tab.
 *
 * Combines browsing (previously separate in the CLI) into one natural flow: 1.
 * Pick a movie from the left panel list 2. Pick a showtime from the dropdown 3.
 * Choose number of seats and attendee type per seat 4. Apply optional seat
 * filters 5. Click "Search Seats" — available seats appear 6. Select your seats
 * from the list 7. Click "Confirm & Pay"
 *
 * MVC role: View + Controller for the booking workflow. All business logic
 * stays in MakeBookingService and BrowsingService.
 *
 * Location: src/cinemaapp/gui/BrowseBookPanel.java
 */
public class BrowseBookPanel extends JPanel {

    private final BrowsingService browsingService;
    private final MakeBookingService makeBookingService;
    private final String username;

    // State
    private List<MovieDetails> allMovieDetails = new ArrayList<>();
    private List<Seat> availableSeats = new ArrayList<>();
    private ShowInfo selectedShowInfo;

    // Left panel
    private DefaultListModel<String> movieListModel = new DefaultListModel<>();
    private JList<String> movieJList;

    // Right panel
    private JTextArea detailsArea;
    private JComboBox<String> showtimeCombo;
    private JSpinner numSeatsSpinner;
    private JPanel attendeePanel;       // rebuilt on seat count change
    private List<JComboBox<AttendeeType>> attendeeCombos = new ArrayList<>();

    // Filters
    private JCheckBox aisleCheck;
    private JCheckBox accessibleCheck;
    private JCheckBox rowRangeCheck;
    private JTextField minRowField;
    private JTextField maxRowField;

    // Seat selection
    private DefaultListModel<String> seatListModel = new DefaultListModel<>();
    private JList<String> seatJList;
    private JLabel totalLabel;
    private JButton confirmBtn;

    public BrowseBookPanel(BrowsingService browsingService,
            MakeBookingService makeBookingService,
            String username) {
        this.browsingService = browsingService;
        this.makeBookingService = makeBookingService;
        this.username = username;
        buildUI();
        loadMovies();
    }

    // ── UI construction ───────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Left: movie list
        JPanel leftPanel = new JPanel(new BorderLayout(0, 6));
        leftPanel.setPreferredSize(new Dimension(210, 0));
        leftPanel.setBorder(new TitledBorder("Now Showing"));

        movieJList = new JList<>(movieListModel);
        movieJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        movieJList.setFont(new Font("Arial", Font.PLAIN, 13));
        movieJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onMovieSelected();
            }
        });

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.addActionListener(e -> loadMovies());

        leftPanel.add(new JScrollPane(movieJList), BorderLayout.CENTER);
        leftPanel.add(refreshBtn, BorderLayout.SOUTH);

        // Right: booking workflow
        JPanel rightPanel = buildRightPanel();

        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(210);
        split.setResizeWeight(0.0);
        add(split, BorderLayout.CENTER);
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(4, 10, 4, 4));

        // Movie details
        p.add(sectionLabel("Movie Details"));
        detailsArea = new JTextArea(4, 40);
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setBackground(getBackground());
        JScrollPane detailScroll = new JScrollPane(detailsArea);
        detailScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        detailScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(detailScroll);
        p.add(vgap(8));

        // Showtime
        p.add(sectionLabel("Showtime"));
        showtimeCombo = new JComboBox<>();
        showtimeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        showtimeCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        showtimeCombo.addActionListener(e -> onShowtimeSelected());
        p.add(showtimeCombo);
        p.add(vgap(8));

        // Number of seats
        p.add(sectionLabel("Number of Seats"));
        numSeatsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        numSeatsSpinner.setMaximumSize(new Dimension(70, 28));
        numSeatsSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        numSeatsSpinner.addChangeListener(e -> rebuildAttendeeCombos());
        p.add(numSeatsSpinner);
        p.add(vgap(8));

        // Attendee types
        p.add(sectionLabel("Attendee Type per Seat"));
        attendeePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        attendeePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(attendeePanel);
        rebuildAttendeeCombos();
        p.add(vgap(8));

        // Filters
        p.add(sectionLabel("Seat Filters  (optional)"));
        p.add(buildFilterRow());
        p.add(vgap(6));

        JButton searchBtn = new JButton("Search Available Seats");
        searchBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchBtn.addActionListener(e -> searchSeats());
        p.add(searchBtn);
        p.add(vgap(8));

        // Available seats
        p.add(sectionLabel("Available Seats  (Ctrl+click to select multiple)"));
        seatJList = new JList<>(seatListModel);
        seatJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        seatJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateTotal();
            }
        });
        JScrollPane seatScroll = new JScrollPane(seatJList);
        seatScroll.setPreferredSize(new Dimension(0, 110));
        seatScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        seatScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(seatScroll);
        p.add(vgap(8));

        // Total + confirm
        totalLabel = new JLabel("Total: —");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(totalLabel);
        p.add(vgap(6));

        confirmBtn = new JButton("✔  Confirm & Pay");
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBtn.setEnabled(false);
        confirmBtn.setBackground(new Color(46, 139, 87));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setFont(confirmBtn.getFont().deriveFont(Font.BOLD, 13f));
        confirmBtn.addActionListener(e -> confirmBooking());
        p.add(confirmBtn);

        JScrollPane scroll = new JScrollPane(p);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scroll, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildFilterRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        aisleCheck = new JCheckBox("Aisle");
        accessibleCheck = new JCheckBox("Accessible");
        rowRangeCheck = new JCheckBox("Row range:");
        minRowField = new JTextField("A", 2);
        maxRowField = new JTextField("E", 2);

        minRowField.setEnabled(false);
        maxRowField.setEnabled(false);
        rowRangeCheck.addActionListener(e -> {
            minRowField.setEnabled(rowRangeCheck.isSelected());
            maxRowField.setEnabled(rowRangeCheck.isSelected());
        });

        row.add(aisleCheck);
        row.add(accessibleCheck);
        row.add(rowRangeCheck);
        row.add(new JLabel("Min:"));
        row.add(minRowField);
        row.add(new JLabel("Max:"));
        row.add(maxRowField);
        return row;
    }

    // ── Data loading ──────────────────────────────────────────────────────────
    private void loadMovies() {
        movieListModel.clear();
        allMovieDetails.clear();
        try {
            for (Movie m : browsingService.getMovies()) {
                MovieDetails md = browsingService.getMovieDetails(m.getMovieId());
                if (md != null) {
                    allMovieDetails.add(md);
                    movieListModel.addElement(
                            "[" + m.getRating() + "] " + m.getTitle());
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading movies: " + e.getMessage());
        }
    }

    // ── Event handlers ────────────────────────────────────────────────────────
    private void onMovieSelected() {
        int idx = movieJList.getSelectedIndex();
        if (idx < 0 || idx >= allMovieDetails.size()) {
            return;
        }

        MovieDetails md = allMovieDetails.get(idx);
        Movie m = md.getMovie();

        detailsArea.setText(
                m.getTitle() + "  [" + m.getRating() + "]\n"
                + m.getRuntime() + " minutes\n\n"
                + m.getDescription()
        );

        showtimeCombo.removeAllItems();
        for (ShowInfo si : md.getShowtimes()) {
            showtimeCombo.addItem(
                    si.getShowtime().getDateTime()
                    + "  |  Screen: " + si.getShowtime().getScreenId()
                    + "  |  Available: " + si.getAvailSeats()
                    + "  |  $" + String.format("%.2f", si.getShowtime().getBasePrice())
            );
        }

        seatListModel.clear();
        totalLabel.setText("Total: —");
        confirmBtn.setEnabled(false);
        rebuildAttendeeCombos();
    }

    private void onShowtimeSelected() {
        int mIdx = movieJList.getSelectedIndex();
        int sIdx = showtimeCombo.getSelectedIndex();
        if (mIdx < 0 || sIdx < 0 || sIdx >= allMovieDetails.get(mIdx).getShowtimes().size()) {
            selectedShowInfo = null;
            return;
        }
        selectedShowInfo = allMovieDetails.get(mIdx).getShowtimes().get(sIdx);
    }

    private void rebuildAttendeeCombos() {
        attendeePanel.removeAll();
        attendeeCombos.clear();
        int count = (int) numSeatsSpinner.getValue();
        for (int i = 0; i < count; i++) {
            attendeePanel.add(new JLabel("Seat " + (i + 1) + ":"));
            JComboBox<AttendeeType> combo = new JComboBox<>(AttendeeType.values());
            attendeeCombos.add(combo);
            attendeePanel.add(combo);
        }
        attendeePanel.revalidate();
        attendeePanel.repaint();
    }

    private void searchSeats() {
        onShowtimeSelected();
        if (selectedShowInfo == null) {
            JOptionPane.showMessageDialog(this, "Please select a showtime first.");
            return;
        }

        List<SeatFilter> filters = new ArrayList<>();
        if (aisleCheck.isSelected()) {
            filters.add(new AisleFilter(true));
        }
        if (accessibleCheck.isSelected()) {
            filters.add(new AccessibleFilter(true));
        }
        if (rowRangeCheck.isSelected()) {
            String min = minRowField.getText().trim().toUpperCase();
            String max = maxRowField.getText().trim().toUpperCase();
            if (!min.isEmpty() && !max.isEmpty()) {
                filters.add(new RowRangeFilter(min.charAt(0), max.charAt(0)));
            }
        }

        availableSeats = makeBookingService.getFilteredSeats(
                selectedShowInfo.getShowtime().getShowtimeId(), filters);

        seatListModel.clear();
        for (Seat s : availableSeats) {
            seatListModel.addElement(
                    s.getSeatId()
                    + "  Row " + s.getRow()
                    + (s.isNearAisle() ? "  [Aisle]" : "")
                    + (s.isAccessible() ? "  [Accessible]" : "")
            );
        }

        if (availableSeats.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No seats match your criteria. Try different filters.");
        }
        totalLabel.setText("Total: —");
        confirmBtn.setEnabled(false);
    }

    private void updateTotal() {
        if (selectedShowInfo == null) {
            return;
        }
        int[] indices = seatJList.getSelectedIndices();
        if (indices.length == 0) {
            totalLabel.setText("Total: —");
            confirmBtn.setEnabled(false);
            return;
        }

        double total = 0;
        for (int i = 0; i < indices.length && i < attendeeCombos.size(); i++) {
            AttendeeType type = (AttendeeType) attendeeCombos.get(i).getSelectedItem();
            total += selectedShowInfo.getShowtime().getBasePrice()
                    * type.getPriceModifier();
        }

        totalLabel.setText(String.format(
                "Total: $%.2f  (%d seat(s) selected)", total, indices.length));

        int required = (int) numSeatsSpinner.getValue();
        confirmBtn.setEnabled(indices.length == required);
    }

    private void confirmBooking() {
        onShowtimeSelected();
        if (selectedShowInfo == null) {
            return;
        }

        int[] indices = seatJList.getSelectedIndices();
        int required = (int) numSeatsSpinner.getValue();

        if (indices.length != required) {
            JOptionPane.showMessageDialog(this,
                    "Please select exactly " + required + " seat(s).");
            return;
        }

        // Map selected indices to Seat objects
        List<Seat> seatsToBook = new ArrayList<>();
        List<AttendeeType> selectedTypes = new ArrayList<>();
        for (int i = 0; i < indices.length; i++) {
            seatsToBook.add(availableSeats.get(indices[i]));
            AttendeeType t = (i < attendeeCombos.size())
                    ? (AttendeeType) attendeeCombos.get(i).getSelectedItem()
                    : AttendeeType.ADULT;
            selectedTypes.add(t);
        }

        // Calculate total for confirmation dialog
        double total = 0;
        for (int i = 0; i < seatsToBook.size(); i++) {
            total += selectedShowInfo.getShowtime().getBasePrice()
                    * selectedTypes.get(i).getPriceModifier();
        }

        int pay = JOptionPane.showConfirmDialog(this,
                String.format("Confirm payment of $%.2f for %d seat(s)?",
                        total, seatsToBook.size()),
                "Confirm Payment", JOptionPane.YES_NO_OPTION);

        if (pay != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(this, "Payment declined — booking cancelled.");
            return;
        }

        List<BookingItem> cart = new ArrayList<>();
        for (int i = 0; i < seatsToBook.size(); i++) {
            cart.add(new BookingItem(
                    seatsToBook.get(i).getSeatId(),
                    selectedTypes.get(i),
                    selectedShowInfo.getShowtime().getBasePrice() * selectedTypes.get(i).getPriceModifier()
            ));
        }
        String bookingCode = makeBookingService.makeBooking(
                selectedShowInfo.getShowtime().getShowtimeId(), cart);

        if (bookingCode == null) {
            JOptionPane.showMessageDialog(this,
                    "Booking failed. A selected seat may have just been taken.",
                    "Booking Failed", JOptionPane.ERROR_MESSAGE);
        } else {
            showConfirmationDialog(bookingCode, cart);
            seatListModel.clear();
            availableSeats.clear();
            totalLabel.setText("Total: —");
            confirmBtn.setEnabled(false);
            loadMovies();   // refresh available seat counts
        }
    }

    private void showConfirmationDialog(String bookingCode, List<BookingItem> cart) {
    StringBuilder msg = new StringBuilder();
    msg.append("✅  Booking Confirmed!\n\n");
    msg.append("Booking Code : ").append(bookingCode).append("\n\n");
    msg.append("Seats:\n");
    for (BookingItem item : cart) {
        msg.append("  • ").append(item.getSeatId())
                .append("  [").append(item.getAttendeeType()).append("]")
                .append("  $").append(String.format("%.2f", item.getItemPrice()))
                .append("\n");
    }
    double total = cart.stream().mapToDouble(BookingItem::getItemPrice).sum();
    msg.append(String.format("\nTotal Paid: $%.2f", total));
    msg.append("\n\nView your ticket in the 'My Bookings' tab.");

    JOptionPane.showMessageDialog(this, msg.toString(),
            "Booking Confirmed ✅", JOptionPane.INFORMATION_MESSAGE);
}

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }
}
