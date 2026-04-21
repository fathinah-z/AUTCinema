package cinemaapp.model;

public class Movie {

    private String movieId;
    private String title;
    private MovieRating rating;
    private String description;
    private int runtime;

    public Movie(String movieId, String title, MovieRating rating, String description, int runtime) {
        this.movieId = movieId;
        this.title = title;
        this.rating = rating;
        this.description = description;
        this.runtime = runtime;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MovieRating getRating() {
        return rating;
    }

    public void setRating(MovieRating rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %d min", title, rating, runtime);
    }
}
