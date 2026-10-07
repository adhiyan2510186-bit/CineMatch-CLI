package cinematch.model;

/**
 * Abstract base class for any catalog item. Holds shared metadata and
 * enforces validation through its setters.
 */
public abstract class Media {

    private int id;
    private String title;
    private String genre;
    private double imdbRating;
    private int durationMinutes;

    protected Media(int id, String title, String genre, double imdbRating, int durationMinutes) {
        setId(id);
        setTitle(title);
        setGenre(genre);
        setImdbRating(imdbRating);
        setDurationMinutes(durationMinutes);
    }

    /** Prints a formatted description of this item. Overridden per media type. */
    public abstract void displayDetails();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be positive: " + id);
        }
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        this.title = title.trim();
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre must not be blank");
        }
        this.genre = genre.trim();
    }

    public double getImdbRating() {
        return imdbRating;
    }

    public void setImdbRating(double imdbRating) {
        if (imdbRating < 0.0 || imdbRating > 10.0) {
            throw new IllegalArgumentException("IMDb rating must be between 0 and 10: " + imdbRating);
        }
        this.imdbRating = imdbRating;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Duration must be positive: " + durationMinutes);
        }
        this.durationMinutes = durationMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return id == ((Media) o).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "#" + id + " " + title;
    }
}
