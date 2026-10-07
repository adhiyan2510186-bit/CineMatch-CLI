package cinematch.strategy;

import cinematch.model.Movie;
import cinematch.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Hard filter: only movies of one genre, highest IMDb first. */
public class GenreFilterStrategy implements RecommendationStrategy {

    private final String genre;

    public GenreFilterStrategy(String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            throw new IllegalArgumentException("Genre must not be blank");
        }
        this.genre = genre.trim();
    }

    @Override
    public List<Movie> recommend(List<Movie> catalog, User user, int limit) {
        List<Movie> matches = new ArrayList<>();
        for (Movie movie : catalog) {
            if (movie.getGenre().equalsIgnoreCase(genre)) {
                matches.add(movie);
            }
        }
        matches.sort(Comparator.comparingDouble(Movie::getImdbRating).reversed());
        return matches.subList(0, Math.min(limit, matches.size()));
    }

    @Override
    public String getName() {
        return "Genre: " + genre;
    }
}
