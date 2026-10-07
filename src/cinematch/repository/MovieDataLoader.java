package cinematch.repository;

import cinematch.model.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parses the movie catalog CSV:
 * ID,Title,Genre,Director,LeadActors,Keywords,IMDbRating,Duration
 * Actors and keywords are pipe-separated. Fields may be double-quoted to contain commas.
 */
public class MovieDataLoader {

    private static final int EXPECTED_COLUMNS = 8;
    /** Splits on commas that are not inside double quotes. */
    private static final Pattern CSV_SPLIT = Pattern.compile(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    private static final Pattern PIPE_SPLIT = Pattern.compile("\\|");

    public List<Movie> load(Path csvFile) throws IOException {
        List<Movie> movies = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // header
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    movies.add(parseLine(line));
                } catch (IllegalArgumentException e) {
                    // NumberFormatException is an IllegalArgumentException too
                    System.err.printf("Skipping malformed row %d in %s: %s%n",
                            lineNumber, csvFile.getFileName(), e.getMessage());
                }
            }
        }
        return movies;
    }

    Movie parseLine(String line) {
        String[] fields = CSV_SPLIT.split(line, -1);
        if (fields.length != EXPECTED_COLUMNS) {
            throw new IllegalArgumentException(
                    "expected " + EXPECTED_COLUMNS + " columns but found " + fields.length);
        }
        for (int i = 0; i < fields.length; i++) {
            fields[i] = unquote(fields[i]);
        }
        return new Movie(
                Integer.parseInt(fields[0]),
                fields[1],
                fields[2],
                fields[3],
                splitPipes(fields[4]),
                splitPipes(fields[5]),
                Double.parseDouble(fields[6]),
                Integer.parseInt(fields[7]));
    }

    private static String unquote(String field) {
        String trimmed = field.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    private static List<String> splitPipes(String field) {
        if (field.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(PIPE_SPLIT.split(field)));
    }
}
