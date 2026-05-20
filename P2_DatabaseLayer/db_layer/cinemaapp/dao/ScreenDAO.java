package cinemaapp.dao;

import cinemaapp.model.Screen;
import java.sql.SQLException;

/** Data-Access Object interface for {@link Screen}. */
public interface ScreenDAO {
    Screen findById(String screenId) throws SQLException;
}
