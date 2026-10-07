# CineMatch CLI

CineMatch CLI is a terminal movie recommender written in Java. It combines your own ratings, IMDb scores and matches on genre, cast and themes to rank movies. It also has a "Surprise Me!" mode and a movie-night planner. The full specification is in [`cinematch_cli_project_specification.md`](cinematch_cli_project_specification.md), and the class diagram is in [`docs/uml-class-diagram.md`](docs/uml-class-diagram.md).

**Requirements:** Java 8 or newer, and JUnit 5 for the tests.

## Project layout

```
src/cinematch/            application sources (sources root = src)
  Main.java               entry point and menu loop
  model/                  Media (abstract), Movie, User
  strategy/               RecommendationStrategy + Hybrid / GenreFilter / SurpriseMe
  repository/             MovieDataLoader (CSV), UserDataRepository (ratings + watchlist)
  service/                RecommendationEngine (search, strategies, planner)
test/cinematch/test/      JUnit 5 tests (test sources root = test)
resources/movies.csv      the 42-movie catalog
data/                     user_ratings.csv and watchlist.txt, created on first save
docs/                     UML class diagram
```

## Running from an IDE

1. **Open the project folder.**
   - **IntelliJ:** Open, then pick the folder. Mark `src` as **Sources Root** and `test` as **Test Sources Root**.
   - **Eclipse:** create a Java project on this folder, then add `src` and `test` as source folders.
   - **VS Code:** with the Extension Pack for Java, the folders are picked up automatically.
2. **Add JUnit 5** to the project. In IntelliJ, hover over `@Test` in `RecommendationEngineTest`, then pick *Add 'JUnit5' to classpath*.
3. **Run `cinematch.Main`** with the working directory set to the **project root**. The app finds `resources/movies.csv` and `data/` using paths relative to that directory.
4. **Run `RecommendationEngineTest`** to execute the test suite.

## Running from the command line

```
javac -d out src/cinematch/Main.java src/cinematch/*/*.java
java -cp out cinematch.Main
```

## Features

- **Profiles.** Enter a username to load your saved ratings and watchlist. Press Enter instead to use Guest mode, which saves nothing.
- **Recommend for me.** Movies are ranked by this score:
  `MatchScore = Base + Genre Boost + Actor Boost + Theme Boost`
  - **Base:** your own rating, or IMDb / 2 if you haven't rated the movie.
  - **Boosts:** taken from the movies you rated 4 stars or higher. A shared genre adds +1.0, any shared lead actor adds +1.0, and each shared theme keyword adds +0.5.
  - Movies you have watched are left out.
- **Search.** Find movies by genre, director, actor, theme or title. Matching ignores case and accepts partial text, so "nolan" finds Christopher Nolan.
- **Surprise Me!**
  - **Wildcard:** a random movie you haven't seen.
  - **Hidden Gem:** a movie you haven't seen, rated IMDb 8.0 or higher, from outside your favorite genre.
- **Ratings and watchlist.** Pick a movie by its ID or by typing part of its title (you choose from a list if several match). Rate it from 1.0 to 5.0 stars, which also marks it as watched. You can add and remove watchlist entries and mark movies watched or unwatched.
- **Movie night planner.** Enter the minutes you have free. The planner picks the set of unwatched movies with the highest total match score that fits that time.
- **Saving.** Data is saved to `data/` when you exit or switch profile. It is also saved if you quit with Ctrl+C.
