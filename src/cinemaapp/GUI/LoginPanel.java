package cinemaapp.GUI;

import cinemaapp.dao.AccountDAO;
import cinemaapp.model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * Login and registration screen shown when the application starts.
 *
 * This is a brand new file — it does NOT replace CLIController.
 * It lives in:  src/cinemaapp/gui/LoginPanel.java
 *
 * The {@code onSuccess} Consumer is injected by MainFrame.
 * When credentials are verified, LoginPanel calls onSuccess.accept(account)
 * and has no further knowledge of what happens — loose coupling via callback.
 *
 * MVC role: View + Controller for authentication.
 * Uses AccountDAO directly (no service needed for simple credential check).
 */
public class LoginPanel extends JPanel {

    private final AccountDAO        accountDAO;
    private final Consumer<Account> onSuccess;   // callback to MainFrame

    private JTextField     usernameField;
    private JPasswordField passwordField;
    private JLabel         feedbackLabel;

    public LoginPanel(AccountDAO accountDAO, Consumer<Account> onSuccess) {
        this.accountDAO = accountDAO;
        this.onSuccess  = onSuccess;
        buildUI();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        setLayout(new GridBagLayout());
        setBackground(new Color(18, 18, 30));

        // Central card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(new Color(38, 38, 58));
        card.setBorder(new EmptyBorder(40, 52, 40, 52));

        // Title
        JLabel title = centreLabel("🎬  AUT Cinema", Font.BOLD, 22, Color.WHITE);
        JLabel sub   = centreLabel("Sign in to continue", Font.PLAIN, 13,
                                    new Color(160, 160, 180));

        // Fields
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        styleField(usernameField);
        styleField(passwordField);

        // Feedback
        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(feedbackLabel.getFont().deriveFont(12f));
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JButton loginBtn    = makeButton("Log In",   new Color(60, 120, 200));
        JButton registerBtn = makeButton("Register", new Color(55, 55, 75));

        loginBtn.addActionListener(e    -> handleLogin());
        registerBtn.addActionListener(e -> handleRegister());

        // Enter key triggers login
        getRootPane().setDefaultButton(loginBtn);

        // Assemble
        card.add(title);
        card.add(vgap(4));
        card.add(sub);
        card.add(vgap(28));
        card.add(fieldLabel("Username"));
        card.add(vgap(4));
        card.add(usernameField);
        card.add(vgap(14));
        card.add(fieldLabel("Password"));
        card.add(vgap(4));
        card.add(passwordField);
        card.add(vgap(22));
        card.add(loginBtn);
        card.add(vgap(8));
        card.add(registerBtn);
        card.add(vgap(14));
        card.add(feedbackLabel);

        add(card);
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }
        try {
            Account account = accountDAO.authenticate(username, password);
            if (account != null) {
                showSuccess("Login successful — welcome, " + username + "!");
                onSuccess.accept(account);       // hand control to MainFrame
            } else {
                showError("Incorrect username or password.");
            }
        } catch (SQLException e) {
            showError("Database error: " + e.getMessage());
        }
    }

    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password to register.");
            return;
        }
        if (username.length() < 3) {
            showError("Username must be at least 3 characters.");
            return;
        }
        if (password.length() < 4) {
            showError("Password must be at least 4 characters.");
            return;
        }
        try {
            if (accountDAO.usernameExists(username)) {
                showError("That username is already taken. Please choose another.");
                return;
            }
            accountDAO.register(username, password);
            Account account = accountDAO.authenticate(username, password);
            showSuccess("Account created! Welcome, " + username);
            onSuccess.accept(account);
        } catch (SQLException e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showError(String msg) {
        feedbackLabel.setForeground(new Color(240, 80, 80));
        feedbackLabel.setText(msg);
    }

    private void showSuccess(String msg) {
        feedbackLabel.setForeground(new Color(80, 200, 100));
        feedbackLabel.setText(msg);
    }

    private JLabel centreLabel(String text, int style, float size, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(style, size));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setForeground(new Color(180, 180, 200));
        lbl.setFont(lbl.getFont().deriveFont(12f));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setBackground(new Color(52, 52, 72));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(88, 88, 120)),
                new EmptyBorder(4, 10, 4, 10)));
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private Component vgap(int height) {
        return Box.createVerticalStrut(height);
    }
}