package cinematch.strategy;

import cinematch.model.Movie;
import cinematch.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Serendipity picks, never including anything the user has rated or watched.
 * <ul>
 *   <li>WILDCARD: any unseen movie at random.</li>
 *   <li>HIDDEN_GEM: an unseen movie with IMDb &ge; 8.0 outside the user's
 *       primary favorite genre.</li>
 * </ul>
 */
public class SurpriseMeStrategy implements RecommendationStrategy {

    public enum Mode { WILDCARD, HIDDEN_GEM }

    public static final double HIDDEN_GEM_MIN_IMDB = 8.0;

    private final Mode mode;
    private final Random random;

    public SurpriseMeStrategy(Mode mode) {
        this(mode, new Random());
    }

    public SurpriseMeStrategy(Mode mode, Random random) {
        this.mode = mode;
        this.random = random;
    }

    @Override
    public List<Movie> recommend(List<Movie> catalog, User user, int limit) {
        String favoriteGenre = mode == Mode.HIDDEN_GEM
                ? new PreferenceProfile(user, catalog, -1).getPrimaryGenre()
                : null;

        List<Movie> candidates = new ArrayList<>();
        for (Movie movie : catalog) {
            if (user.hasSeen(movie.getId())) {
                continue;
            }
            if (mode == Mode.HIDDEN_GEM) {
                if (movie.getImdbRating() < HIDDEN_GEM_MIN_IMDB) {
                    continue;
                }
                if (favoriteGenre != null && movie.getGenre().equalsIgnoreCase(favoriteGenre)) {
                    continue;
                }
            }
            candidates.add(movie);
        }
        Collections.shuffle(candidates, random);
        return candidates.subList(0, Math.min(limit, candidates.size()));
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public String getName() {
        return mode == Mode.WILDCARD ? "Surprise Me! (Wildcard)" : "Surprise Me! (Hidden Gem)";
    }
}
