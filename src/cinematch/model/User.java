package cinematch.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A viewer profile. Owns its personal ratings, watchlist and watched history
 * (stored as movie IDs so persistence stays independent of the catalog).
 */
public class User {

    public static final double MIN_RATING = 1.0;
    public static final double MAX_RATING = 5.0;
    public static final String GUEST_NAME = "Guest";

    private final String username;
    private final boolean guest;
    private final Map<Integer, Double> ratings = new LinkedHashMap<>();
    private final Set<Integer> watchlist = new LinkedHashSet<>();
    private final Set<Integer> watched = new LinkedHashSet<>();

    public User(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (username.contains(",") || username.contains("|")) {
            throw new IllegalArgumentException("Username must not contain ',' or '|'");
        }
        this.username = username.trim();
        this.guest = false;
    }

    private User() {
        this.username = GUEST_NAME;
        this.guest = true;
    }

    public static User guest() {
        return new User();
    }

    public String getUsername() {
        return username;
    }

    public boolean isGuest() {
        return guest;
    }

    public void rate(int movieId, double rating) {
        if (rating < MIN_RATING || rating > MAX_RATING) {
            throw new IllegalArgumentException(
                    String.format("Rating must be between %.1f and %.1f", MIN_RATING, MAX_RATING));
        }
        ratings.put(movieId, rating);
    }

    /** Returns the user's rating for a movie, or null if unrated. */
    public Double getRating(int movieId) {
        return ratings.get(movieId);
    }

    public Map<Integer, Double> getRatings() {
        return Collections.unmodifiableMap(ratings);
    }

    public boolean addToWatchlist(int movieId) {
        return watchlist.add(movieId);
    }

    public boolean removeFromWatchlist(int movieId) {
        return watchlist.remove(movieId);
    }

    public Set<Integer> getWatchlist() {
        return Collections.unmodifiableSet(watchlist);
    }

    public boolean markWatched(int movieId) {
        return watched.add(movieId);
    }

    public boolean markUnwatched(int movieId) {
        return watched.remove(movieId);
    }

    public boolean isWatched(int movieId) {
        return watched.contains(movieId);
    }

    public Set<Integer> getWatched() {
        return Collections.unmodifiableSet(watched);
    }

    /** A movie counts as seen if it has been rated or marked watched. */
    public boolean hasSeen(int movieId) {
        return ratings.containsKey(movieId) || watched.contains(movieId);
    }
}
