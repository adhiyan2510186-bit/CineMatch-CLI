package cinematch.repository;

import cinematch.model.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persists user profiles to two files in a data directory:
 * <ul>
 *   <li>user_ratings.csv - Username,MovieID,Rating</li>
 *   <li>watchlist.txt    - username|movieId|WATCHLIST or username|movieId|WATCHED</li>
 * </ul>
 * Saving a user rewrites only that user's lines; other profiles are preserved.
 */
public class UserDataRepository {

    public static final String RATINGS_FILE = "user_ratings.csv";
    public static final String WATCHLIST_FILE = "watchlist.txt";
    private static final String RATINGS_HEADER = "Username,MovieID,Rating";
    private static final String WATCHLIST_TAG = "WATCHLIST";
    private static final String WATCHED_TAG = "WATCHED";

    private final Path ratingsFile;
    private final Path watchlistFile;

    public UserDataRepository(Path dataDir) {
        this.ratingsFile = dataDir.resolve(RATINGS_FILE);
        this.watchlistFile = dataDir.resolve(WATCHLIST_FILE);
    }

    /** Loads a profile; a user with no saved data starts empty. */
    public User loadUser(String username) throws IOException {
        User user = new User(username);
        String name = user.getUsername();

        for (String line : readLines(ratingsFile)) {
            String[] parts = line.split(",");
            if (parts.length != 3 || !parts[0].trim().equalsIgnoreCase(name)) {
                continue;
            }
            try {
                user.rate(Integer.parseInt(parts[1].trim()), Double.parseDouble(parts[2].trim()));
            } catch (IllegalArgumentException e) {
                System.err.println("Ignoring bad rating line: " + line);
            }
        }

        for (String line : readLines(watchlistFile)) {
            String[] parts = line.split("\\|");
            if (parts.length != 3 || !parts[0].trim().equalsIgnoreCase(name)) {
                continue;
            }
            try {
                int movieId = Integer.parseInt(parts[1].trim());
                String tag = parts[2].trim();
                if (WATCHLIST_TAG.equals(tag)) {
                    user.addToWatchlist(movieId);
                } else if (WATCHED_TAG.equals(tag)) {
                    user.markWatched(movieId);
                }
            } catch (NumberFormatException e) {
                System.err.println("Ignoring bad watchlist line: " + line);
            }
        }
        return user;
    }

    /** Saves a profile. Guest users are never persisted. */
    public void saveUser(User user) throws IOException {
        if (user.isGuest()) {
            return;
        }
        String name = user.getUsername();

        List<String> ratingLines = new ArrayList<>();
        ratingLines.add(RATINGS_HEADER);
        for (String line : readLines(ratingsFile)) {
            if (!belongsTo(line.split(",")[0], name)) {
                ratingLines.add(line);
            }
        }
        for (Map.Entry<Integer, Double> entry : user.getRatings().entrySet()) {
            ratingLines.add(name + "," + entry.getKey() + "," + entry.getValue());
        }

        List<String> watchLines = new ArrayList<>();
        for (String line : readLines(watchlistFile)) {
            if (!belongsTo(line.split("\\|")[0], name)) {
                watchLines.add(line);
            }
        }
        for (int id : user.getWatchlist()) {
            watchLines.add(name + "|" + id + "|" + WATCHLIST_TAG);
        }
        for (int id : user.getWatched()) {
            watchLines.add(name + "|" + id + "|" + WATCHED_TAG);
        }

        if (ratingsFile.getParent() != null) {
            Files.createDirectories(ratingsFile.getParent());
        }
        Files.write(ratingsFile, ratingLines, StandardCharsets.UTF_8);
        Files.write(watchlistFile, watchLines, StandardCharsets.UTF_8);
    }

    private static boolean belongsTo(String owner, String username) {
        return owner.trim().equalsIgnoreCase(username);
    }

    /** Reads non-blank lines, skipping the ratings header. Missing file yields no lines. */
    private static List<String> readLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!Files.exists(file)) {
            return lines;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty() && !line.equals(RATINGS_HEADER)) {
                lines.add(line);
            }
        }
        return lines;
    }
}
