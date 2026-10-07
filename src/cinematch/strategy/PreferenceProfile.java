package cinematch.strategy;

import cinematch.model.Movie;
import cinematch.model.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A user's tastes, derived from the movies they rated highly (at least
 * {@link #LIKED_THRESHOLD} stars). All values are stored lower-case.
 */
public class PreferenceProfile {

    public static final double LIKED_THRESHOLD = 4.0;

    private final Set<String> genres = new HashSet<>();
    private final Set<String> actors = new HashSet<>();
    private final Set<String> keywords = new HashSet<>();
    private final Map<String, Integer> genreCounts = new HashMap<>();
    private final Map<String, Double> genreRatingTotals = new HashMap<>();

    /**
     * Builds a profile from the user's liked movies.
     *
     * @param excludeId a movie to leave out of the profile (so it cannot boost
     *                  itself), or -1 to include everything
     */
    public PreferenceProfile(User user, List<Movie> catalog, int excludeId) {
        for (Movie movie : catalog) {
            Double rating = user.getRating(movie.getId());
            if (movie.getId() == excludeId || rating == null || rating < LIKED_THRESHOLD) {
                continue;
            }
            String genre = normalize(movie.getGenre());
            genres.add(genre);
            genreCounts.merge(genre, 1, Integer::sum);
            genreRatingTotals.merge(genre, rating, Double::sum);
            for (String actor : movie.getLeadActors()) {
                actors.add(normalize(actor));
            }
            for (String keyword : movie.getKeywords()) {
                keywords.add(normalize(keyword));
            }
        }
    }

    public boolean isEmpty() {
        return genres.isEmpty();
    }

    public boolean likesGenre(String genre) {
        return genres.contains(normalize(genre));
    }

    public boolean likesAnyActor(List<String> candidates) {
        for (String actor : candidates) {
            if (actors.contains(normalize(actor))) {
                return true;
            }
        }
        return false;
    }

    public int countMatchingKeywords(List<String> candidates) {
        int matches = 0;
        for (String keyword : candidates) {
            if (keywords.contains(normalize(keyword))) {
                matches++;
            }
        }
        return matches;
    }

    /**
     * The most frequently liked genre (lower-case); ties go to the higher
     * average rating. Returns null when the profile is empty.
     */
    public String getPrimaryGenre() {
        String best = null;
        int bestCount = 0;
        double bestAverage = 0.0;
        for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
            int count = entry.getValue();
            double average = genreRatingTotals.get(entry.getKey()) / count;
            if (count > bestCount || (count == bestCount && average > bestAverage)) {
                best = entry.getKey();
                bestCount = count;
                bestAverage = average;
            }
        }
        return best;
    }

    public Set<String> getGenres() {
        return Collections.unmodifiableSet(genres);
    }

    static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
