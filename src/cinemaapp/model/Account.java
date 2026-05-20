package cinemaapp.model;

/**
 * Represents a user account.
 * The password is intentionally not stored in-memory after authentication;
 * only the username (the session identity) is held.
 */
public class Account {

    private final String username;

    public Account(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return username;
    }
}
