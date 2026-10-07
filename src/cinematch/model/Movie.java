package cinematch.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A feature film with director, lead cast and theme keywords. */
public class Movie extends Media {

    private String director;
    private List<String> leadActors;
    private List<String> keywords;

    public Movie(int id, String title, String genre, String director,
                 List<String> leadActors, List<String> keywords,
                 double imdbRating, int durationMinutes) {
        super(id, title, genre, imdbRating, durationMinutes);
        setDirector(director);
        setLeadActors(leadActors);
        setKeywords(keywords);
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        if (director == null || director.trim().isEmpty()) {
            throw new IllegalArgumentException("Director must not be blank");
        }
        this.director = director.trim();
    }

    public List<String> getLeadActors() {
        return Collections.unmodifiableList(leadActors);
    }

    public void setLeadActors(List<String> leadActors) {
        this.leadActors = cleanList(leadActors);
    }

    public List<String> getKeywords() {
        return Collections.unmodifiableList(keywords);
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = cleanList(keywords);
    }

    /** IMDb rating converted to the 5-star scale used for user ratings. */
    public double getScaledImdb() {
        return getImdbRating() / 2.0;
    }

    @Override
    public void displayDetails() {
        System.out.printf("[%d] %s (%s) - dir. %s%n", getId(), getTitle(), getGenre(), director);
        System.out.printf("     IMDb %.1f | %d min | Cast: %s%n",
                getImdbRating(), getDurationMinutes(), String.join(", ", leadActors));
        System.out.printf("     Themes: %s%n", String.join(", ", keywords));
    }

    private static List<String> cleanList(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    cleaned.add(value.trim());
                }
            }
        }
        return cleaned;
    }
}
