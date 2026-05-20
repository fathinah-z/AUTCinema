package cinemaapp.repository;

import cinemaapp.dao.ScreenDAO;
import cinemaapp.model.Screen;

import java.sql.SQLException;

/** Adapts {@link ScreenDAO} to the {@link ScreenRepository} interface. */
public class DbScreenRepository implements ScreenRepository {

    private final ScreenDAO screenDAO;

    public DbScreenRepository(ScreenDAO screenDAO) {
        this.screenDAO = screenDAO;
    }

    @Override
    public Screen findById(String screenId) {
        try {
            return screenDAO.findById(screenId);
        } catch (SQLException e) {
            throw new RuntimeException("DB error – findById screen: " + screenId, e);
        }
    }
}
