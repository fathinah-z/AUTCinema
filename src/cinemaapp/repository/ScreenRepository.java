package cinemaapp.repository;

import cinemaapp.model.Screen;

public interface ScreenRepository {
    Screen findById(String screenId);
}
