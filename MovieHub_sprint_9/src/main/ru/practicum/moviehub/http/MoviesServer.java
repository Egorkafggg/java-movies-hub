package ru.practicum.moviehub.http;

import com.google.gson.JsonParseException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesServer {
    private static final int MIN_YEAR = 1888;
    private static final int MAX_YEAR = LocalDate.now().getYear() + 1;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final String YEAR_ERROR_MESSAGE = "год должен быть между " + MIN_YEAR + " и " + MAX_YEAR;
    private static final String TITLE_LENGTH_ERROR_MESSAGE = "название не должно превышать " + MAX_TITLE_LENGTH + " символов";
    private static final String INVALID_JSON_ERROR_MESSAGE = "Некорректный JSON";

    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store, int port) {
        this.store = store;
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/movies", new MoviesHandler());
        } catch (IOException e) {
            throw new RuntimeException("Не удалось запустить сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен на порту " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getStore() {
        return store;
    }

    private class MoviesHandler extends BaseHttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            try {
                if (path.equals("/movies")) {
                    handleMoviesCollection(exchange, method, query);
                } else if (path.matches("/movies/\\d+") || path.matches("/movies/[^/]+")) {
                    handleMovieById(exchange, method, path);
                } else {
                    sendError(exchange, 404, "Ресурс не найден");
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Внутренняя ошибка сервера");
            }
        }

        private void handleMoviesCollection(HttpExchange exchange, String method, String query) throws IOException {
            switch (method) {
                case "GET":
                    handleGetMovies(exchange, query);
                    break;
                case "POST":
                    handlePostMovie(exchange);
                    break;
                default:
                    sendMethodNotAllowed(exchange);
            }
        }

        private void handleGetMovies(HttpExchange exchange, String query) throws IOException {
            if (query != null && !query.isEmpty()) {
                if (query.startsWith("year=")) {
                    String yearStr = query.substring(5);
                    try {
                        int year = Integer.parseInt(yearStr);
                        List<Movie> movies = store.getByYear(year);
                        sendResponse(exchange, 200, movies);
                    } catch (NumberFormatException e) {
                        sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                    }
                } else {
                    sendError(exchange, 400, "Некорректный параметр запроса — 'year'");
                }
            } else {
                List<Movie> movies = store.getAll();
                sendResponse(exchange, 200, movies);
            }
        }

        private void handlePostMovie(HttpExchange exchange) throws IOException {
            if (!isJsonContentType(exchange)) {
                sendUnsupportedMediaType(exchange);
                return;
            }

            String body = readRequestBody(exchange);
            Movie movie;
            try {
                movie = gson.fromJson(body, Movie.class);
            } catch (JsonParseException e) {
                sendError(exchange, 400, INVALID_JSON_ERROR_MESSAGE);
                return;
            }

            if (movie == null) {
                sendError(exchange, 400, INVALID_JSON_ERROR_MESSAGE);
                return;
            }

            List<String> errors = validateMovie(movie);
            if (!errors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", errors);
                sendResponse(exchange, 422, errorResponse);
                return;
            }

            Movie created = store.add(movie);
            sendResponse(exchange, 201, created);
        }

        private List<String> validateMovie(Movie movie) {
            List<String> errors = new ArrayList<>();

            if (movie.getTitle() == null || movie.getTitle().trim().isEmpty()) {
                errors.add("название не должно быть пустым");
            } else if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
                errors.add(TITLE_LENGTH_ERROR_MESSAGE);
            }

            if (movie.getYear() == null || movie.getYear() < MIN_YEAR || movie.getYear() > MAX_YEAR) {
                errors.add(YEAR_ERROR_MESSAGE);
            }

            return errors;
        }

        private void handleMovieById(HttpExchange exchange, String method, String path) throws IOException {
            String idStr = path.substring("/movies/".length());

            long id;
            try {
                id = Long.parseLong(idStr);
            } catch (NumberFormatException e) {
                sendError(exchange, 400, "Некорректный ID");
                return;
            }

            switch (method) {
                case "GET":
                    handleGetMovieById(exchange, id);
                    break;
                case "DELETE":
                    handleDeleteMovie(exchange, id);
                    break;
                default:
                    sendMethodNotAllowed(exchange);
            }
        }

        private void handleGetMovieById(HttpExchange exchange, long id) throws IOException {
            Optional<Movie> movie = store.getById(id);
            if (movie.isPresent()) {
                sendResponse(exchange, 200, movie.get());
            } else {
                sendError(exchange, 404, "Фильм не найден");
            }
        }

        private void handleDeleteMovie(HttpExchange exchange, long id) throws IOException {
            if (store.delete(id)) {
                sendNoContent(exchange);
            } else {
                sendError(exchange, 404, "Фильм не найден");
            }
        }
    }
}