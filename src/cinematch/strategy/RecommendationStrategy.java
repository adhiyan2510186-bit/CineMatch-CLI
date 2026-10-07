package cinematch.strategy;

import cinematch.model.Movie;
import cinematch.model.User;

import java.util.List;

/** An interchangeable recommendation algorithm. */
public interface RecommendationStrategy {

    /** Returns up to {@code limit} movies from the catalog for this user, best first. */
    List<Movie> recommend(List<Movie> catalog, User user, int limit);

    String getName();
}
