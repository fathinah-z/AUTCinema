package cinemaapp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that manages the single embedded Apache Derby connection.
 *
 * Using the Singleton pattern guarantees only one Connection is active at a
 * time, which is correct for Derby Embedded (it does not support concurrent
 * connections from multiple JVMs, but a single shared connection within one
 * JVM is fine for a desktop application of this scale).
 */
public final class DatabaseManager {

    private static final String DB_URL =
            "jdbc:derby:AUTCinemaDB;user=comp603;password=comp603";

    // The one and only instance – created lazily on first access.
    private static DatabaseManager instance;

    private Connection connection;

    // Private constructor enforces Singleton
    private DatabaseManager() {
        try {
            // Load the embedded Derby driver (required for JDK 25 module path)
            Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver");
        } catch (ClassNotFoundException e) {
            // Driver may auto-load via ServiceLoader; ignore if not found here
        }
    }

    /**
     * Returns the single {@code DatabaseManager} instance (thread-safe via
     * synchronized method – adequate for a single-threaded Swing application).
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Returns a live {@link Connection}, (re)opening it if it has been closed
     * or was never opened.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            // Auto-commit ON so every DAO operation is immediately durable.
            // For multi-step transactions (e.g. booking) callers must disable
            // auto-commit themselves and commit/rollback explicitly.
            connection.setAutoCommit(true);
        }
        return connection;
    }

    /**
     * Gracefully shuts down the embedded Derby engine.
     * Must be called once when the application exits.
     */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }

        try {
            // Derby shutdown throws a specific SQLException with state 08006
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            if ("XJ015".equals(e.getSQLState())) {
                System.out.println("Derby shut down normally.");
            } else {
                System.err.println("Derby shutdown warning: " + e.getMessage());
            }
        }
    }
}
