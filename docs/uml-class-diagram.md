# CineMatch CLI — UML Class Diagram

```mermaid
classDiagram
    direction TB

    class Media {
        <<abstract>>
        -int id
        -String title
        -String genre
        -double imdbRating
        -int durationMinutes
        #Media(id, title, genre, imdbRating, durationMinutes)
        +displayDetails()* void
        +getId() int
        +getTitle() String
        +getGenre() String
        +getImdbRating() double
        +getDurationMinutes() int
    }

    class Movie {
        -String director
        -List~String~ leadActors
        -List~String~ keywords
        +getDirector() String
        +getLeadActors() List~String~
        +getKeywords() List~String~
        +getScaledImdb() double
        +displayDetails() void
    }

    class User {
        -String username
        -boolean guest
        -Map~Integer, Double~ ratings
        -Set~Integer~ watchlist
        -Set~Integer~ watched
        +guest()$ User
        +rate(movieId, rating) void
        +getRating(movieId) Double
        +addToWatchlist(movieId) boolean
        +removeFromWatchlist(movieId) boolean
        +markWatched(movieId) boolean
        +markUnwatched(movieId) boolean
        +hasSeen(movieId) boolean
    }

    class RecommendationStrategy {
        <<interface>>
        +recommend(catalog, user, limit) List~Movie~
        +getName() String
    }

    class HybridRecommendation {
        +GENRE_BOOST$ double = 1.0
        +ACTOR_BOOST$ double = 1.0
        +KEYWORD_BOOST$ double = 0.5
        +score(movie, user, catalog) double
        +scoreUnwatched(catalog, user) Map~Movie, Double~
        +recommend(catalog, user, limit) List~Movie~
    }

    class GenreFilterStrategy {
        -String genre
        +recommend(catalog, user, limit) List~Movie~
    }

    class SurpriseMeStrategy {
        -Mode mode
        -Random random
        +recommend(catalog, user, limit) List~Movie~
    }

    class Mode {
        <<enumeration>>
        WILDCARD
        HIDDEN_GEM
    }

    class PreferenceProfile {
        -Set~String~ genres
        -Set~String~ actors
        -Set~String~ keywords
        +likesGenre(genre) boolean
        +likesAnyActor(actors) boolean
        +countMatchingKeywords(keywords) int
        +getPrimaryGenre() String
    }

    class MovieDataLoader {
        +load(Path csvFile) List~Movie~
    }

    class UserDataRepository {
        -Path ratingsFile
        -Path watchlistFile
        +loadUser(username) User
        +saveUser(user) void
    }

    class RecommendationEngine {
        -List~Movie~ catalog
        -Map~Integer, Movie~ index
        -RecommendationStrategy strategy
        +setStrategy(strategy) void
        +recommend(user, limit) List~Movie~
        +score(movie, user) double
        +findById(id) Movie
        +searchByGenre(query) List~Movie~
        +searchByDirector(query) List~Movie~
        +searchByActor(query) List~Movie~
        +searchByTheme(query) List~Movie~
        +planMovieNight(user, minutes) List~Movie~
    }

    class Main {
        -RecommendationEngine engine
        -UserDataRepository userRepository
        -User currentUser
        +main(args)$ void
    }

    Media <|-- Movie : inheritance
    RecommendationStrategy <|.. HybridRecommendation : realizes
    RecommendationStrategy <|.. GenreFilterStrategy : realizes
    RecommendationStrategy <|.. SurpriseMeStrategy : realizes
    SurpriseMeStrategy +-- Mode
    HybridRecommendation ..> PreferenceProfile : uses
    SurpriseMeStrategy ..> PreferenceProfile : uses

    RecommendationEngine o-- "1" RecommendationStrategy : active strategy
    RecommendationEngine o-- "*" Movie : catalog
    MovieDataLoader ..> Movie : creates
    UserDataRepository ..> User : loads / saves

    Main --> RecommendationEngine
    Main --> UserDataRepository
    Main --> MovieDataLoader
    Main --> "1" User : current profile
```

## Relationships and OOP principles

| Principle | Where it appears |
|---|---|
| **Abstraction** | `Media` is an abstract class with the abstract method `displayDetails()`. `RecommendationStrategy` is an interface. |
| **Inheritance** | `Movie extends Media`. |
| **Polymorphism** | `Movie` overrides `displayDetails()`. `RecommendationEngine.setStrategy()` swaps Hybrid, Genre Filter and Surprise Me! strategies while the program runs, and `recommend()` calls whichever one is active. |
| **Encapsulation** | Every model field is `private`. The setters validate their input (for example, a rating must be 1.0–5.0 and a duration must be positive), and collection getters return unmodifiable views. |
| **Composition** | A `User` owns its ratings map, watchlist set and watched set, which are created and destroyed with it. |
| **Strategy pattern** | `RecommendationEngine` (the context) delegates to a `RecommendationStrategy`. |
