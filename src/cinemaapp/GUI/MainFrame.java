package cinemaapp.GUI;

import cinemaapp.dao.DAOFactory;
import cinemaapp.model.Account;
import cinemaapp.repository.DbBookingRepository;
import cinemaapp.service.*;

import javax.swing.*;
import java.awt.*;

/**
 * Root application window.
 *
 * Uses CardLayout to switch between: "login" card → LoginPanel (shown on
 * startup) "main" card → JTabbedPane with three tabs
 *
*/
public class MainFrame extends JFrame {

    private static final String CARD_LOGIN = "login";
    private static final String CARD_MAIN = "main";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private final DAOFactory factory;
    private final DbBookingRepository bookingRepo;
    private final BrowsingService browsingService;
    private final MakeBookingService makeBookingService;
    private final CancelBookingService cancelService;

    private Account currentAccount;

    // FIX 3+4: stored as a field so onLogout() can remove it by reference
    private JPanel mainCard;

    public MainFrame(DAOFactory factory,
            DbBookingRepository bookingRepo,
            BrowsingService browsingService,
            MakeBookingService makeBookingService,
            CancelBookingService cancelService) {

        this.factory = factory;
        this.bookingRepo = bookingRepo;
        this.browsingService = browsingService;
        this.makeBookingService = makeBookingService;
        this.cancelService = cancelService;

        setTitle("AUT Cinema Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 700);
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        root.add(new LoginPanel(factory.getAccountDAO(), this::onLogin), CARD_LOGIN);
        setContentPane(root);
        cardLayout.show(root, CARD_LOGIN);
    }

    // ── Login callback ─────────────────────────────────────────────────────────
    private void onLogin(Account account) {
        this.currentAccount = account;
        bookingRepo.setCurrentUsername(account.getUsername());

        // Build MyBookingsPanel first — the other two panels need its
        // loadBookings() method as a Runnable callback (Observer pattern).
        MyBookingsPanel myBookings = new MyBookingsPanel(
                factory.getBookingDAO(),
                factory.getMovieDAO(),
                factory.getShowtimeDAO(),
                account.getUsername());

        BrowseBookPanel browseBook = new BrowseBookPanel(
                browsingService, makeBookingService, account.getUsername());

        CancelPanel cancelPanel = new CancelPanel(
                cancelService, bookingRepo,
                factory.getMovieDAO(),
                factory.getShowtimeDAO());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(tabs.getFont().deriveFont(Font.PLAIN, 13f));
        tabs.addTab("🎬  Browse & Book", browseBook);
        tabs.addTab("❌  Cancel Booking", cancelPanel);
        tabs.addTab("📋  My Bookings", myBookings);

        // FIX 4: mainCard is a field, not a local variable
        mainCard = new JPanel(new BorderLayout());
        mainCard.add(buildHeader(), BorderLayout.NORTH);
        mainCard.add(tabs, BorderLayout.CENTER);

        root.add(mainCard, CARD_MAIN);
        cardLayout.show(root, CARD_MAIN);
        setTitle("AUT Cinema — " + account.getUsername());
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    private void onLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Log out of your account?", "Log Out", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        currentAccount = null;
        bookingRepo.setCurrentUsername("guest");

        // FIX 3: remove by stored field reference — no instanceof needed
        if (mainCard != null) {
            root.remove(mainCard);
            mainCard = null;
        }
        root.revalidate();
        root.repaint();
        cardLayout.show(root, CARD_LOGIN);
        setTitle("AUT Cinema Booking System");
    }

    // ── Header bar ────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(28, 28, 46));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        JLabel lbl = new JLabel(
                "🎬  AUT Cinema   |   Logged in as: " + currentAccount.getUsername());
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 13f));

        JButton logoutBtn = new JButton("Log Out");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> onLogout());

        bar.add(lbl, BorderLayout.WEST);
        bar.add(logoutBtn, BorderLayout.EAST);
        return bar;
    }
}
