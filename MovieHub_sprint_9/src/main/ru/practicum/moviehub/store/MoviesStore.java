package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Long, Movie> movies = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Movie add(Movie movie) {
        long id = idGenerator.getAndIncrement();
        movie.setId(id);
        movies.put(id, movie);
        return movie;
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> getById(Long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean delete(Long id) {
        return movies.remove(id) != null;
    }

    public List<Movie> getByYear(Integer year) {
        return movies.values().stream()
                .filter(movie -> movie.getYear().equals(year))
                .collect(Collectors.toList());
    }

    public void clear() {
        movies.clear();
        idGenerator.set(1);
    }
}