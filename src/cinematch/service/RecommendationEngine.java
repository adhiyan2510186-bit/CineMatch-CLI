package cinematch.service;

import cinematch.model.Movie;
import cinematch.model.User;
import cinematch.strategy.HybridRecommendation;
import cinematch.strategy.RecommendationStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Coordinates the movie catalog with the active recommendation strategy,
 * and provides searching and the movie-night planner.
 */
public class RecommendationEngine {

    public static final int MAX_PLANNER_MINUTES = 1440;

    private final List<Movie> catalog;
    private final Map<Integer, Movie> index = new LinkedHashMap<>();
    private final HybridRecommendation hybrid = new HybridRecommendation();
    private RecommendationStrategy strategy;

    public RecommendationEngine(List<Movie> catalog) {
        this.catalog = new ArrayList<>(catalog);
        for (Movie movie : catalog) {
            index.put(movie.getId(), movie);
        }
        this.strategy = hybrid;
    }

    public void setStrategy(RecommendationStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy must not be null");
        }
        this.strategy = strategy;
    }

    public RecommendationStrategy getStrategy() {
        return strategy;
    }

    public List<Movie> recommend(User user, int limit) {
        return strategy.recommend(catalog, user, limit);
    }

    /** Hybrid match score for one movie, regardless of the active strategy. */
    public double score(Movie movie, User user) {
        return hybrid.score(movie, user, catalog);
    }

    public List<Movie> getCatalog() {
        return Collections.unmodifiableList(catalog);
    }

    /** Returns the movie with this ID, or null if it is not in the catalog. */
    public Movie findById(int id) {
        return index.get(id);
    }

    public List<Movie> searchByTitle(String query) {
        return search(query, m -> Collections.singletonList(m.getTitle()));
    }

    public List<Movie> searchByGenre(String query) {
        return search(query, m -> Collections.singletonList(m.getGenre()));
    }

    public List<Movie> searchByDirector(String query) {
        return search(query, m -> Collections.singletonList(m.getDirector()));
    }

    public List<Movie> searchByActor(String query) {
        return search(query, Movie::getLeadActors);
    }

    public List<Movie> searchByTheme(String query) {
        return search(query, Movie::getKeywords);
    }

    private List<Movie> search(String query, Function<Movie, List<String>> fields) {
        List<Movie> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        for (Movie movie : catalog) {
            for (String value : fields.apply(movie)) {
                if (value.toLowerCase(Locale.ROOT).contains(needle)) {
                    results.add(movie);
                    break;
                }
            }
        }
        return results;
    }

    /**
     * Picks the set of unwatched movies with the highest total match score whose
     * combined runtime fits within {@code minutes} (0/1 knapsack).
     */
    public List<Movie> planMovieNight(User user, int minutes) {
        if (minutes < 1 || minutes > MAX_PLANNER_MINUTES) {
            throw new IllegalArgumentException(
                    "Available time must be between 1 and " + MAX_PLANNER_MINUTES + " minutes");
        }
        Map<Movie, Double> scores = hybrid.scoreUnwatched(catalog, user);
        List<Movie> items = new ArrayList<>(scores.keySet());
        int n = items.size();

        // Values scaled to integers (hundredths of a point) to keep the DP exact.
        int[][] best = new int[n + 1][minutes + 1];
        for (int i = 1; i <= n; i++) {
            Movie movie = items.get(i - 1);
            int weight = movie.getDurationMinutes();
            int value = (int) Math.round(scores.get(movie) * 100);
            for (int t = 0; t <= minutes; t++) {
                best[i][t] = best[i - 1][t];
                if (weight <= t && best[i - 1][t - weight] + value > best[i][t]) {
                    best[i][t] = best[i - 1][t - weight] + value;
                }
            }
        }

        List<Movie> playlist = new ArrayList<>();
        int t = minutes;
        for (int i = n; i >= 1; i--) {
            if (best[i][t] != best[i - 1][t]) {
                Movie movie = items.get(i - 1);
                playlist.add(movie);
                t -= movie.getDurationMinutes();
            }
        }
        Collections.reverse(playlist);
        return playlist;
    }
}
