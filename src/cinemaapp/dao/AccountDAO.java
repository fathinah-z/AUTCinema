package cinemaapp.dao;

import cinemaapp.model.Account;
import java.sql.SQLException;

/** Data-Access Object interface for {@link Account}. */
public interface AccountDAO {
    /** Returns the Account if credentials match, otherwise null. */
    Account authenticate(String username, String password) throws SQLException;

    /** Persists a new account. Throws if username already exists. */
    void register(String username, String password) throws SQLException;

    boolean usernameExists(String username) throws SQLException;
}
