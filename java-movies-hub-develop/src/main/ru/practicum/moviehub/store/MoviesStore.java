package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {
    private long id = 0;
    private Map<Long, Movie> moviesMap = new HashMap<>();


    public void addMovie(String title, int year) {
        Movie newMovie = new Movie(title, year);
        boolean movieExists = moviesMap.values().stream()
                .anyMatch(existingMovie -> existingMovie.equals(newMovie));
        if (!movieExists) {
            id++;
            moviesMap.put(id, newMovie);
        }
    }

    public void addMovie(Movie newMovie) {
        boolean movieExists = moviesMap.values().stream()
                .anyMatch(existingMovie -> existingMovie.equals(newMovie));
        if (!movieExists) {
            id++;
            moviesMap.put(id, newMovie);
        }
    }

    public List<Movie> getMoviesByYear(int year) {
        if (moviesMap.isEmpty()) {
            return new ArrayList<>();
        }
        List<Movie> moviesOfYear = moviesMap.values().stream()
                .filter(movie -> movie.getYear() == year)
                .collect(Collectors.toList());
        if (moviesOfYear.isEmpty()) {
            return new ArrayList<>();
        } else {
            return moviesOfYear;
        }
    }

    public Optional<Movie> getMovieById(long id) {
        if (moviesMap.isEmpty()) {
            return Optional.empty();
        }

        if (moviesMap.containsKey(id)) {
            return Optional.of(moviesMap.get(id));
        } else {
            return Optional.empty();
        }
    }

    public void deleteMovieById(long id) {
        if (!moviesMap.isEmpty()) {
            moviesMap.remove(id);
        }
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(moviesMap.values());
    }

    public void clearStore() {
        id = 0;
        moviesMap.clear();
    }
}