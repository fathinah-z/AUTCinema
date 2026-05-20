package cinemaapp.dao.derby;

import cinemaapp.dao.AccountDAO;
import cinemaapp.db.DatabaseManager;
import cinemaapp.model.Account;

import java.sql.*;

/**
 * Apache Derby implementation of {@link AccountDAO}.
 *
 * Passwords are stored as plain text here to match the CUI project's
 * simplicity.  In a production system these would be salted+hashed (e.g.
 * BCrypt).
 */
public class DerbyAccountDAO implements AccountDAO {

    private final DatabaseManager dbManager;

    public DerbyAccountDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Account authenticate(String username, String password) throws SQLException {
        String sql = "SELECT username FROM Account WHERE username = ? AND password = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getString("username"));
                }
            }
        }
        return null; // credentials did not match
    }

    @Override
    public void register(String username, String password) throws SQLException {
        String sql = "INSERT INTO Account (username, password) VALUES (?, ?)";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Account WHERE username = ?";

        try (PreparedStatement ps = dbManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }
}
