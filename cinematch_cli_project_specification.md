# CineMatch CLI - Advanced Movie Recommendation System

## 1. Executive Overview & Problem Statement
* **Application Title:** CineMatch CLI
* **Target Environment:** Java SE (Standard Edition) | Command Line Interface (CLI)
* **Problem Statement:** Streaming platform users often experience decision paralysis due to overwhelming catalog sizes and generic recommendation algorithms. Standard algorithms either rely purely on external ratings (ignoring individual user tastes) or strictly on user history (creating filter bubbles).
* **Solution:** **CineMatch CLI** is an interactive Java-based terminal application that delivers personalized movie recommendations using a **Hybrid Recommendation Scoring Engine**. It seamlessly blends personal user ratings, fallback IMDb dataset scores, multi-attribute content matching (actors, themes, directors, styles), and serendipitous wildcard picks ("Surprise Me!").
* **Persistence & Storage:** File-based data persistence utilizing local CSV and plain text files.

---

## 2. Project Features & Functionality

### Core Features
1. **Interactive CLI Menu System:**
   * User-friendly console menu loop with robust input validation and friendly prompts.
   * Profile switching / Guest mode support.

2. **Multi-Attribute CSV Catalog Processing:**
   * Reads a rich dataset (`movies.csv`) containing English-language titles, genres, directors, cast members, keywords/themes, runtime, and baseline IMDb ratings.

3. **Hybrid Recommendation Engine:**
   * Dynamic scoring based on a hybrid formula:
     $$\text{MatchScore} = \text{Base Rating} + \text{Genre Boost} + \text{Actor Boost} + \text{Theme/Style Boost}$$
   * **Base Score:** Uses personal user rating ($1.0 - 5.0$) if rated; falls back to IMDb rating scaled to 5 stars ($\text{IMDb} / 2$) if unrated.
   * **Preference Pattern Boosts:** Analyzes historical user ratings ($\ge 4.0$ stars) to automatically calculate point bonuses for favorite genres ($+1.0$), matching cast members ($+1.0$), and matching themes/styles ($+0.5$ per keyword).

4. **Multi-Criteria Catalog Searching & Filtering:**
   * Search/filter by Genre, Director, Lead Actor, or Specific Theme/Style.

5. **"Surprise Me!" (Serendipity Feature):**
   * **Pure Wildcard:** Picks a completely random unseen title from the catalog.
   * **Hidden Gem Strategy:** Recommends a high-quality movie ($\text{IMDb} \ge 8.0$) outside the user's primary favorite genre to encourage genre exploration.

6. **Watchlist & User Ratings Management:**
   * Rate movies on a 1.0 to 5.0 scale.
   * Add movies to a personal Watchlist.
   * Mark titles as "Watched" / "Unwatched".

7. **Binge-Time / Movie Night Planner (Bonus Feature):**
   * Input available free time (in minutes) and get an optimized playlist of recommended movies fitting that duration limit without overflowing.

8. **File-Based Data Storage & Persistence:**
   * Automatically saves user watchlists and personal rating logs to `user_ratings.csv` and `watchlist.txt` upon exit and reloads them at launch.

---

## 3. Object-Oriented Software Design & Architecture

### Package Structure
```text
cinematch/
│
├── model/                     --> Core Data Models
│   ├── Media.java             --> Abstract Parent Class
│   ├── Movie.java             --> Subclass extending Media
│   ├── TVShow.java            --> Subclass extending Media
│   └── User.java              --> Profile, Ratings Map & Watchlist
│
├── strategy/                  --> Design Pattern: Strategy Pattern
│   ├── RecommendationStrategy.java --> Interface
│   ├── HybridRecommendation.java   --> Multi-attribute content scoring
│   ├── GenreFilterStrategy.java  --> Hard filtering by genre
│   └── SurpriseMeStrategy.java    --> Serendipity / Wildcard generator
│
├── repository/                --> File I/O & Persistence Layer
│   ├── MovieDataLoader.java     --> Parses movies.csv (regex CSV loader)
│   └── UserDataRepository.java  --> Reads/Writes user ratings & watchlists
│
├── service/                   --> Business Logic Layer
│   └── RecommendationEngine.java--> Coordinates catalog and active strategies
│
├── test/                      --> Automated Unit Validation
│   └── RecommendationEngineTest.java --> JUnit test suite
│
└── Main.java                  --> Application Entry Point & CLI Loop
```

### OOP Principles Applied

* **Abstraction:** 
  * `Media` serves as an abstract base class hiding shared metadata handling.
  * `RecommendationStrategy` provides an interface abstraction for interchangeable algorithms.
* **Inheritance:** 
  * `Movie` and `TVShow` inherit from `Media`.
* **Polymorphism:** 
  * `Media.displayDetails()` is overridden across media types for custom output formatting.
  * Runtime strategy selection allows dynamic switching between Hybrid Scoring and "Surprise Me!" modes.
* **Encapsulation:** 
  * Fields in all model classes are `private`. Data modifications occur strictly through validated getter and setter methods.
* **Composition:** 
  * A `User` object maintains a composition relationship with lists of `Media` objects and maps of user ratings.

---

## 4. Dataset Structure (`movies.csv`)

File path: `resources/movies.csv`

```csv
ID,Title,Genre,Director,LeadActors,Keywords,IMDbRating,Duration
1,Inception,Sci-Fi,Christopher Nolan,Leonardo DiCaprio|Joseph Gordon-Levitt,Mind-Bending|Heist|Dreams,8.8,148
2,Interstellar,Sci-Fi,Christopher Nolan,Matthew McConaughey|Anne Hathaway,Space|Time Travel|Emotional,8.7,169
3,The Dark Knight,Action,Christopher Nolan,Christian Bale|Heath Ledger,Gritty|Superhero|Crime,9.0,152
4,Pulp Fiction,Crime,Quentin Tarantino,John Travolta|Uma Thurman,Non-Linear|Gangster|Cult,8.9,154
5,Shutter Island,Mystery,Martin Scorsese,Leonardo DiCaprio|Mark Ruffalo,Psychological|Twist|Dark,8.2,138
6,The Shawshank Redemption,Drama,Frank Darabont,Tim Robbins|Morgan Freeman,Hope|Prison|Friendship,9.3,142
7,Forrest Gump,Drama,Robert Zemeckis,Tom Hanks|Robin Wright,Inspirational|History|Life,8.8,142
8,Gladiator,Action,Ridley Scott,Russell Crowe|Joaquin Phoenix,Revenge|Ancient Rome|Epic,8.5,155
```

---

## 5. JUnit Testing Plan

The `RecommendationEngineTest.java` class verifies critical functionalities using JUnit 5:

1. **`testCSVLoading()`**: Ensures `MovieDataLoader` correctly parses rows, handles pipe-separated actors/keywords, and constructs valid `Movie` objects.
2. **`testPersonalRatingOverridesIMDb()`**: Confirms that personal user ratings take priority over IMDb baseline scores during hybrid scoring calculation.
3. **`testActorAndThemeBonusBoosting()`**: Verifies that movies matching a user's high-rated actors/themes receive accuracy score boosts.
4. **`testSurpriseMeExcludesWatched()`**: Asserts that the "Surprise Me!" strategy never recommends a movie already rated or marked watched by the user.
5. **`testDurationPlanner()`**: Verifies that the Movie Night Planner playlist total runtime does not exceed user-specified time constraints.

---

## 6. Implementation Checklist & Deliverables

- [x] Problem statement, scope, and objective definition.
- [ ] Implement core entities (`Media`, `Movie`, `User`).
- [ ] Implement file readers and writers (`MovieDataLoader`, `UserDataRepository`).
- [ ] Implement `RecommendationStrategy` implementations (`HybridRecommendation`, `SurpriseMeStrategy`).
- [ ] Build interactive `Scanner` menu loop with exception handling in `Main.java`.
- [ ] Write JUnit test suite in `RecommendationEngineTest.java`.
- [ ] Create UML Class Diagram for final submission documentation.