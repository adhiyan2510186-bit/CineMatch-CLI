package cinematch.test;

import cinematch.model.Movie;
import cinematch.model.User;
import cinematch.repository.MovieDataLoader;
import cinematch.repository.UserDataRepository;
import cinematch.service.RecommendationEngine;
import cinematch.strategy.HybridRecommendation;
import cinematch.strategy.SurpriseMeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {

    private static final double EPSILON = 1e-9;

    @TempDir
    Path tempDir;

    private List<Movie> catalog;
    private RecommendationEngine engine;
    private HybridRecommendation hybrid;

    @BeforeEach
    void setUp() {
        catalog = new ArrayList<>();
        catalog.add(movie(1, "Inception", "Sci-Fi", "Christopher Nolan",
                list("Leonardo DiCaprio", "Joseph Gordon-Levitt"), list("Mind-Bending", "Heist", "Dreams"), 8.8, 148));
        catalog.add(movie(2, "Interstellar", "Sci-Fi", "Christopher Nolan",
                list("Matthew McConaughey", "Anne Hathaway"), list("Space", "Time Travel", "Emotional"), 8.7, 169));
        catalog.add(movie(3, "The Dark Knight", "Action", "Christopher Nolan",
                list("Christian Bale", "Heath Ledger"), list("Gritty", "Superhero", "Crime"), 9.0, 152));
        catalog.add(movie(4, "Shutter Island", "Mystery", "Martin Scorsese",
                list("Leonardo DiCaprio", "Mark Ruffalo"), list("Psychological", "Twist", "Dreams"), 8.2, 138));
        catalog.add(movie(5, "The Shawshank Redemption", "Drama", "Frank Darabont",
                list("Tim Robbins", "Morgan Freeman"), list("Hope", "Prison", "Friendship"), 9.3, 142));
        catalog.add(movie(6, "Toy Story", "Animation", "John Lasseter",
                list("Tom Hanks", "Tim Allen"), list("Friendship", "Family", "Toys"), 8.3, 81));
        catalog.add(movie(7, "Superbad", "Comedy", "Greg Mottola",
                list("Jonah Hill", "Michael Cera"), list("Party", "Coming of Age"), 7.6, 113));
        catalog.add(movie(8, "The Matrix", "Sci-Fi", "Lana Wachowski",
                list("Keanu Reeves", "Laurence Fishburne"), list("Mind-Bending", "Heist", "Cyberpunk"), 8.7, 136));
        engine = new RecommendationEngine(catalog);
        hybrid = new HybridRecommendation();
    }

    @Test
    void testCSVLoading() throws Exception {
        Path csv = tempDir.resolve("movies.csv");
        Files.write(csv, Arrays.asList(
                "ID,Title,Genre,Director,LeadActors,Keywords,IMDbRating,Duration",
                "1,Inception,Sci-Fi,Christopher Nolan,Leonardo DiCaprio|Joseph Gordon-Levitt,Mind-Bending|Heist|Dreams,8.8,148",
                "2,\"Monsters, Inc.\",Animation,Pete Docter,Billy Crystal|John Goodman,Friendship|Family,8.1,92",
                "3,Broken Row,Drama,Nobody,Someone,Theme,not-a-number,100",
                "4,Too,Few,Columns",
                ""), StandardCharsets.UTF_8);

        List<Movie> movies = new MovieDataLoader().load(csv);

        assertEquals(2, movies.size(), "malformed rows should be skipped");
        Movie inception = movies.get(0);
        assertEquals(1, inception.getId());
        assertEquals("Inception", inception.getTitle());
        assertEquals("Sci-Fi", inception.getGenre());
        assertEquals("Christopher Nolan", inception.getDirector());
        assertEquals(list("Leonardo DiCaprio", "Joseph Gordon-Levitt"), inception.getLeadActors());
        assertEquals(list("Mind-Bending", "Heist", "Dreams"), inception.getKeywords());
        assertEquals(8.8, inception.getImdbRating(), EPSILON);
        assertEquals(148, inception.getDurationMinutes());
        assertEquals("Monsters, Inc.", movies.get(1).getTitle(), "quoted commas should be kept");
    }

    @Test
    void testPersonalRatingOverridesIMDb() {
        User user = new User("alice");
        Movie shawshank = engine.findById(5);

        assertEquals(4.65, hybrid.score(shawshank, user, catalog), EPSILON, "unrated uses IMDb / 2");

        user.rate(5, 2.0);
        assertEquals(2.0, hybrid.score(shawshank, user, catalog), EPSILON, "personal rating wins over IMDb");
    }

    @Test
    void testActorAndThemeBonusBoosting() {
        User user = new User("bob");
        user.rate(1, 5.0); // Inception: Sci-Fi, DiCaprio, Mind-Bending|Heist|Dreams

        // Shutter Island shares DiCaprio (+1.0) and "Dreams" (+0.5); different genre.
        assertEquals(4.1 + 1.0 + 0.5, hybrid.score(engine.findById(4), user, catalog), EPSILON);

        // The Matrix shares the genre (+1.0) and two keywords (+0.5 each).
        assertEquals(4.35 + 1.0 + 1.0, hybrid.score(engine.findById(8), user, catalog), EPSILON);

        // Shawshank shares nothing: base score only.
        assertEquals(4.65, hybrid.score(engine.findById(5), user, catalog), EPSILON);

        // Low ratings do not create preferences.
        User critic = new User("critic");
        critic.rate(1, 2.0);
        assertEquals(4.1, hybrid.score(engine.findById(4), critic, catalog), EPSILON);

        // The boosts lift the matching movie above a higher-IMDb control.
        assertEquals(8, hybrid.recommend(catalog, user, 1).get(0).getId());
    }

    @Test
    void testSurpriseMeExcludesWatched() {
        User user = new User("carol");
        user.rate(1, 5.0);
        user.rate(2, 4.0);
        user.markWatched(5);
        user.markWatched(6);

        for (SurpriseMeStrategy.Mode mode : SurpriseMeStrategy.Mode.values()) {
            SurpriseMeStrategy strategy = new SurpriseMeStrategy(mode, new Random(42));
            for (int i = 0; i < 100; i++) {
                for (Movie pick : strategy.recommend(catalog, user, catalog.size())) {
                    assertFalse(user.hasSeen(pick.getId()), mode + " returned seen movie " + pick);
                }
            }
        }

        for (Movie movie : catalog) {
            user.markWatched(movie.getId());
        }
        assertTrue(new SurpriseMeStrategy(SurpriseMeStrategy.Mode.WILDCARD, new Random(1))
                .recommend(catalog, user, 5).isEmpty());
    }

    @Test
    void testHiddenGemAvoidsFavoriteGenre() {
        User user = new User("dave");
        user.rate(1, 5.0); // Sci-Fi
        user.rate(2, 4.5); // Sci-Fi -> primary genre

        List<Movie> gems = new SurpriseMeStrategy(SurpriseMeStrategy.Mode.HIDDEN_GEM, new Random(7))
                .recommend(catalog, user, catalog.size());

        assertFalse(gems.isEmpty());
        for (Movie gem : gems) {
            assertNotEquals("Sci-Fi", gem.getGenre());
            assertTrue(gem.getImdbRating() >= SurpriseMeStrategy.HIDDEN_GEM_MIN_IMDB);
        }
    }

    @Test
    void testDurationPlanner() {
        User user = new User("erin");
        for (int limit : new int[] {81, 150, 200, 300, 500, 1440}) {
            List<Movie> playlist = engine.planMovieNight(user, limit);
            assertFalse(playlist.isEmpty(), "something should fit in " + limit + " minutes");
            assertTrue(totalRuntime(playlist) <= limit, "playlist overflows " + limit + " minutes");
        }

        assertTrue(engine.planMovieNight(user, 80).isEmpty(), "shortest movie is 81 minutes");

        user.markWatched(6); // Toy Story is the only movie under 100 minutes
        assertTrue(engine.planMovieNight(user, 100).isEmpty(), "watched movies are not planned");

        assertThrows(IllegalArgumentException.class, () -> engine.planMovieNight(user, 0));
    }

    @Test
    void testSearchByTitle() {
        assertEquals(1, engine.searchByTitle("STORY").size(), "case-insensitive");
        assertEquals("Toy Story", engine.searchByTitle("story").get(0).getTitle());
        assertEquals(3, engine.searchByTitle("the").size(), "partial matches");
        assertTrue(engine.searchByTitle("Titanic").isEmpty());
        assertTrue(engine.searchByTitle("  ").isEmpty());
    }

    @Test
    void testRatingValidation() {
        User user = new User("frank");
        assertThrows(IllegalArgumentException.class, () -> user.rate(1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> user.rate(1, 5.5));
        assertNull(user.getRating(1));
    }

    @Test
    void testUserDataRoundTrip() throws Exception {
        UserDataRepository repository = new UserDataRepository(tempDir);

        User alice = new User("alice");
        alice.rate(1, 4.5);
        alice.addToWatchlist(3);
        alice.markWatched(1);
        repository.saveUser(alice);

        User bob = new User("bob");
        bob.rate(2, 3.0);
        repository.saveUser(bob);

        repository.saveUser(User.guest()); // must not be written

        User loaded = repository.loadUser("alice");
        assertEquals(4.5, loaded.getRating(1), EPSILON);
        assertTrue(loaded.getWatchlist().contains(3));
        assertTrue(loaded.isWatched(1));
        assertEquals(3.0, repository.loadUser("bob").getRating(2), EPSILON, "other profiles are kept");
        assertTrue(repository.loadUser(User.GUEST_NAME).getRatings().isEmpty());
    }

    private static int totalRuntime(List<Movie> movies) {
        int total = 0;
        for (Movie movie : movies) {
            total += movie.getDurationMinutes();
        }
        return total;
    }

    private static Movie movie(int id, String title, String genre, String director,
                               List<String> actors, List<String> keywords, double imdb, int duration) {
        return new Movie(id, title, genre, director, actors, keywords, imdb, duration);
    }

    private static List<String> list(String... values) {
        return Arrays.asList(values);
    }
}
