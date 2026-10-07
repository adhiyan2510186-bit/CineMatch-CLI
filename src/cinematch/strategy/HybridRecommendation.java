package cinematch.strategy;

import cinematch.model.Movie;
import cinematch.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MatchScore = Base Rating + Genre Boost + Actor Boost + Theme/Style Boost.
 * <ul>
 *   <li>Base: the user's own rating if present, otherwise IMDb / 2.</li>
 *   <li>Genre: +1.0 if the genre is among the user's liked genres.</li>
 *   <li>Actor: +1.0 if any lead actor appears in a liked movie.</li>
 *   <li>Theme: +0.5 per keyword shared with liked movies.</li>
 * </ul>
 * Boosts come from the user's other liked movies, so a movie never boosts itself.
 */
public class HybridRecommendation implements RecommendationStrategy {

    public static final double GENRE_BOOST = 1.0;
    public static final double ACTOR_BOOST = 1.0;
    public static final double KEYWORD_BOOST = 0.5;

    public double score(Movie movie, User user, List<Movie> catalog) {
        Double personal = user.getRating(movie.getId());
        double score = personal != null ? personal : movie.getScaledImdb();

        PreferenceProfile profile = new PreferenceProfile(user, catalog, movie.getId());
        if (profile.likesGenre(movie.getGenre())) {
            score += GENRE_BOOST;
        }
        if (profile.likesAnyActor(movie.getLeadActors())) {
            score += ACTOR_BOOST;
        }
        score += KEYWORD_BOOST * profile.countMatchingKeywords(movie.getKeywords());
        return score;
    }

    /** Scores every movie the user has not watched. */
    public Map<Movie, Double> scoreUnwatched(List<Movie> catalog, User user) {
        Map<Movie, Double> scores = new HashMap<>();
        for (Movie movie : catalog) {
            if (!user.isWatched(movie.getId())) {
                scores.put(movie, score(movie, user, catalog));
            }
        }
        return scores;
    }

    @Override
    public List<Movie> recommend(List<Movie> catalog, User user, int limit) {
        final Map<Movie, Double> scores = scoreUnwatched(catalog, user);
        List<Movie> ranked = new ArrayList<>(scores.keySet());
        ranked.sort(Comparator.comparingDouble((Movie m) -> scores.get(m))
                .thenComparingDouble(Movie::getImdbRating)
                .reversed());
        return ranked.subList(0, Math.min(limit, ranked.size()));
    }

    @Override
    public String getName() {
        return "Hybrid Match";
    }
}
