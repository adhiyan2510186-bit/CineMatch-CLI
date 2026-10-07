package cinematch;

import cinematch.model.Movie;
import cinematch.model.User;
import cinematch.repository.MovieDataLoader;
import cinematch.repository.UserDataRepository;
import cinematch.service.RecommendationEngine;
import cinematch.strategy.GenreFilterStrategy;
import cinematch.strategy.HybridRecommendation;
import cinematch.strategy.SurpriseMeStrategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** CineMatch CLI entry point and interactive menu loop. */
public class Main {

    private static final Path MOVIES_FILE = Paths.get("resources", "movies.csv");
    private static final Path DATA_DIR = Paths.get("data");
    private static final int RECOMMENDATION_COUNT = 5;
    private static final String DIVIDER = "------------------------------------------------------------";

    private final Scanner in = new Scanner(System.in);
    private final RecommendationEngine engine;
    private final UserDataRepository userRepository;
    private volatile User currentUser;

    private Main(RecommendationEngine engine, UserDataRepository userRepository) {
        this.engine = engine;
        this.userRepository = userRepository;
    }

    public static void main(String[] args) {
        if (!Files.exists(MOVIES_FILE)) {
            System.err.println("Cannot find " + MOVIES_FILE.toAbsolutePath());
            System.err.println("Run CineMatch from the project root folder.");
            System.exit(1);
        }
        List<Movie> catalog;
        try {
            catalog = new MovieDataLoader().load(MOVIES_FILE);
        } catch (IOException e) {
            System.err.println("Failed to read the movie catalog: " + e.getMessage());
            System.exit(1);
            return;
        }
        if (catalog.isEmpty()) {
            System.err.println("The movie catalog is empty.");
            System.exit(1);
        }

        Main app = new Main(new RecommendationEngine(catalog), new UserDataRepository(DATA_DIR));
        Runtime.getRuntime().addShutdownHook(new Thread(app::saveQuietly));
        app.run();
    }

    private void run() {
        System.out.println("============================================================");
        System.out.println("   CineMatch CLI - your personal movie recommender");
        System.out.println("============================================================");
        System.out.println(engine.getCatalog().size() + " movies loaded.");
        try {
            currentUser = login();
            boolean running = true;
            while (running) {
                running = mainMenu();
            }
        } catch (NoSuchElementException e) {
            System.out.println(); // input closed (e.g. Ctrl+D / Ctrl+Z)
        }
        saveQuietly();
        System.out.println("Goodbye!");
    }

    // ---------------------------------------------------------------- menus

    private boolean mainMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("Profile: " + currentUser.getUsername()
                + (currentUser.isGuest() ? " (not saved)" : ""));
        System.out.println(" 1. Recommend for me");
        System.out.println(" 2. Search / filter catalog");
        System.out.println(" 3. Surprise Me!");
        System.out.println(" 4. Rate a movie");
        System.out.println(" 5. Watchlist & watched history");
        System.out.println(" 6. Movie night planner");
        System.out.println(" 7. Switch profile");
        System.out.println(" 0. Save & exit");
        int choice = readInt("Choose an option: ", 0, 7);
        try {
            switch (choice) {
                case 1: showRecommendations(); break;
                case 2: searchMenu(); break;
                case 3: surpriseMenu(); break;
                case 4: rateMovie(); break;
                case 5: watchlistMenu(); break;
                case 6: planMovieNight(); break;
                case 7: switchProfile(); break;
                case 0: return false;
                default: break;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("! " + e.getMessage());
        }
        return true;
    }

    private void showRecommendations() {
        engine.setStrategy(new HybridRecommendation());
        List<Movie> picks = engine.recommend(currentUser, RECOMMENDATION_COUNT);
        if (currentUser.getRatings().isEmpty()) {
            System.out.println("Tip: rate a few movies 4+ stars to personalise these picks.");
        }
        printScored("Top picks for " + currentUser.getUsername(), picks);
    }

    private void searchMenu() {
        System.out.println(" 1. By genre   2. By director   3. By actor   4. By theme/style   0. Back");
        int choice = readInt("Search by: ", 0, 4);
        if (choice == 0) {
            return;
        }
        String query = readLine("Search text: ");
        List<Movie> results;
        switch (choice) {
            case 1:
                engine.setStrategy(new GenreFilterStrategy(query));
                results = engine.recommend(currentUser, Integer.MAX_VALUE);
                if (results.isEmpty()) {
                    results = engine.searchByGenre(query); // allow partial genre names
                }
                break;
            case 2: results = engine.searchByDirector(query); break;
            case 3: results = engine.searchByActor(query); break;
            default: results = engine.searchByTheme(query); break;
        }
        printMovies("Results for \"" + query + "\"", results);
    }

    private void surpriseMenu() {
        System.out.println(" 1. Pure wildcard   2. Hidden gem (outside your favorite genre)   0. Back");
        int choice = readInt("Pick a mode: ", 0, 2);
        if (choice == 0) {
            return;
        }
        SurpriseMeStrategy.Mode mode = choice == 1
                ? SurpriseMeStrategy.Mode.WILDCARD
                : SurpriseMeStrategy.Mode.HIDDEN_GEM;
        engine.setStrategy(new SurpriseMeStrategy(mode));
        List<Movie> pick = engine.recommend(currentUser, 1);
        if (pick.isEmpty()) {
            System.out.println("No unseen movies left to surprise you with!");
            return;
        }
        System.out.println(engine.getStrategy().getName() + " says... try this:");
        pick.get(0).displayDetails();
    }

    private void rateMovie() {
        Movie movie = readMovie();
        Double existing = currentUser.getRating(movie.getId());
        if (existing != null) {
            System.out.printf("You previously rated it %.1f.%n", existing);
        }
        double rating = readDouble("Your rating (1.0 - 5.0): ", User.MIN_RATING, User.MAX_RATING);
        currentUser.rate(movie.getId(), rating);
        currentUser.markWatched(movie.getId());
        System.out.printf("Rated \"%s\" %.1f stars (marked as watched).%n", movie.getTitle(), rating);
    }

    private void watchlistMenu() {
        System.out.println(" 1. View watchlist   2. Add   3. Remove   4. Mark watched   5. Mark unwatched");
        System.out.println(" 6. View watched history   0. Back");
        int choice = readInt("Choose: ", 0, 6);
        Movie movie;
        switch (choice) {
            case 1:
                printMovies("Your watchlist", lookup(currentUser.getWatchlist()));
                break;
            case 2:
                movie = readMovie();
                System.out.println(currentUser.addToWatchlist(movie.getId())
                        ? "Added \"" + movie.getTitle() + "\" to your watchlist."
                        : "\"" + movie.getTitle() + "\" is already on your watchlist.");
                break;
            case 3:
                movie = readMovie();
                System.out.println(currentUser.removeFromWatchlist(movie.getId())
                        ? "Removed \"" + movie.getTitle() + "\" from your watchlist."
                        : "\"" + movie.getTitle() + "\" was not on your watchlist.");
                break;
            case 4:
                movie = readMovie();
                currentUser.markWatched(movie.getId());
                currentUser.removeFromWatchlist(movie.getId());
                System.out.println("Marked \"" + movie.getTitle() + "\" as watched.");
                break;
            case 5:
                movie = readMovie();
                System.out.println(currentUser.markUnwatched(movie.getId())
                        ? "Marked \"" + movie.getTitle() + "\" as unwatched."
                        : "\"" + movie.getTitle() + "\" was not marked watched.");
                break;
            case 6:
                printMovies("Watched history", lookup(currentUser.getWatched()));
                break;
            default:
                break;
        }
    }

    private void planMovieNight() {
        int minutes = readInt("How many minutes do you have? (1-"
                + RecommendationEngine.MAX_PLANNER_MINUTES + "): ", 1, RecommendationEngine.MAX_PLANNER_MINUTES);
        List<Movie> playlist = engine.planMovieNight(currentUser, minutes);
        if (playlist.isEmpty()) {
            System.out.println("Nothing fits in " + minutes + " minutes. Try a longer window!");
            return;
        }
        int total = 0;
        for (Movie movie : playlist) {
            total += movie.getDurationMinutes();
        }
        printScored(String.format("Movie night plan: %d min of %d available", total, minutes), playlist);
    }

    private void switchProfile() {
        saveQuietly();
        currentUser = login();
    }

    // ---------------------------------------------------------------- helpers

    private User login() {
        while (true) {
            String name = readOptionalLine("Enter your username (or press Enter for Guest mode): ");
            if (name.isEmpty()) {
                System.out.println("Welcome, Guest! Your activity will not be saved.");
                return User.guest();
            }
            try {
                User user = userRepository.loadUser(name);
                System.out.printf("Welcome, %s! %d ratings, %d on watchlist.%n",
                        user.getUsername(), user.getRatings().size(), user.getWatchlist().size());
                return user;
            } catch (IllegalArgumentException e) {
                System.out.println("! " + e.getMessage());
            } catch (IOException e) {
                System.out.println("! Could not load saved data (" + e.getMessage() + "); starting fresh.");
                return new User(name);
            }
        }
    }

    private synchronized void saveQuietly() {
        User user = currentUser;
        if (user == null || user.isGuest()) {
            return;
        }
        try {
            userRepository.saveUser(user);
        } catch (IOException e) {
            System.err.println("! Failed to save data for " + user.getUsername() + ": " + e.getMessage());
        }
    }

    private Movie readMovie() {
        while (true) {
            int id = readInt("Movie ID: ", 1, Integer.MAX_VALUE);
            Movie movie = engine.findById(id);
            if (movie != null) {
                return movie;
            }
            System.out.println("! No movie with ID " + id + ". Use Search to find IDs.");
        }
    }

    private List<Movie> lookup(Iterable<Integer> ids) {
        List<Movie> movies = new ArrayList<>();
        for (int id : ids) {
            Movie movie = engine.findById(id);
            if (movie != null) {
                movies.add(movie);
            }
        }
        return movies;
    }

    private void printMovies(String heading, List<Movie> movies) {
        System.out.println();
        System.out.println("== " + heading + " ==");
        if (movies.isEmpty()) {
            System.out.println("(nothing here)");
            return;
        }
        for (Movie movie : movies) {
            movie.displayDetails();
        }
    }

    private void printScored(String heading, List<Movie> movies) {
        System.out.println();
        System.out.println("== " + heading + " ==");
        if (movies.isEmpty()) {
            System.out.println("(nothing here)");
            return;
        }
        for (Movie movie : movies) {
            System.out.printf("Match score %.1f%n", engine.score(movie, currentUser));
            movie.displayDetails();
        }
    }

    private String readOptionalLine(String prompt) {
        System.out.print(prompt);
        return in.nextLine().trim();
    }

    private String readLine(String prompt) {
        while (true) {
            String line = readOptionalLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("! Please type something.");
        }
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.println("! Please enter a whole number"
                    + (max == Integer.MAX_VALUE ? " of at least " + min : " between " + min + " and " + max) + ".");
        }
    }

    private double readDouble(String prompt, double min, double max) {
        while (true) {
            String line = readLine(prompt);
            try {
                double value = Double.parseDouble(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            System.out.printf("! Please enter a number between %.1f and %.1f.%n", min, max);
        }
    }
}
