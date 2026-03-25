package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String endpoint = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        Map<String, String> requestParams = getRequestParams(ex.getRequestURI().getQuery());

        if (method.equalsIgnoreCase("GET")) {
            if (endpoint.split("/").length == 3) {

                try {
                    String id = endpoint.split("/")[2];
                    Optional<Movie> movie = moviesStore.getMovieById(Long.parseLong(id));
                    if (movie.isPresent()) {
                        sendJson(ex, 200, new Gson().toJson(movie.get()));
                    } else {
                        ErrorResponse error = new ErrorResponse("Фильм с ID " + id + " не найден");
                        sendJson(ex, 404, new Gson().toJson(error));
                    }
                } catch (NumberFormatException e) {
                    ErrorResponse error = new ErrorResponse("Некорректный ID. ID должен быть цифрой. Вы ввели: "
                            + endpoint.split("/")[2]);
                    sendJson(ex, 400, new Gson().toJson(error));
                }
            } else if (!requestParams.isEmpty()) {
                String yearString = requestParams.get("year");
                if (yearString != null) {
                    try {
                        int yearInt = Integer.parseInt(yearString);
                        List<Movie> movies = moviesStore.getMoviesByYear(yearInt);
                        if (!(movies == null) || !movies.isEmpty()) {
                            sendJson(ex, 200, new Gson().toJson(movies));
                        } else {
                            sendJson(ex, 200, "[]");
                        }
                    } catch (NumberFormatException e) {
                        ErrorResponse error = new ErrorResponse("Некорректный параметр запроса — year");
                        sendJson(ex, 400, new Gson().toJson(error));
                    }
                } else {
                    List<Movie> movies = moviesStore.getAllMovies();
                    if (movies != null || !movies.isEmpty()) {
                        sendJson(ex, 200, new Gson().toJson(movies));
                    } else {
                        sendJson(ex, 200, "[]");
                    }
                }
            } else {
                if (moviesStore.getAllMovies().isEmpty() || moviesStore.getAllMovies() == null) {
                    List<Movie> movies = moviesStore.getAllMovies();
                    String json = new Gson().toJson(movies);
                    sendJson(ex, 200, json);
                } else {
                    List<Movie> movies = moviesStore.getAllMovies();
                    String json = new Gson().toJson(movies);
                    sendJson(ex, 200, json);
                }
            }
        } else if (method.equalsIgnoreCase("POST")) {
            try {
                String contentType = ex.getRequestHeaders().getFirst("Content-Type");
                if (!"application/json".equals(contentType)) {
                    sendJson(ex, 415, "Unsupported Media Type");
                    return;
                }

                InputStream inputStream = ex.getRequestBody();
                String requestBody = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining());
                inputStream.close();

                JsonObject jsonObject = new Gson().fromJson(requestBody, JsonObject.class);
                String title = jsonObject.get("title").getAsString();
                int year = jsonObject.get("year").getAsInt();

                List<String> inputErrors = getInputErrors(title, year);

                if (!inputErrors.isEmpty()) {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка ввода", inputErrors);
                    sendJson(ex, 422, new Gson().toJson(errorResponse));
                    return;
                }

                Movie newMovie = new Movie(title, year);
                moviesStore.addMovie(newMovie);
                sendJson(ex, 201, new Gson().toJson(newMovie));

            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректное значение года");
                sendJson(ex, 400, new Gson().toJson(errorResponse));
            }
        } else if (method.equalsIgnoreCase("DELETE") && endpoint.split("/").length == 3) {

            try {
                String id = endpoint.split("/")[2];
                if (moviesStore.getMovieById(Long.parseLong(id)).isPresent()) {
                    moviesStore.deleteMovieById(Long.parseLong(id));
                    sendNoContent(ex);
                } else {
                    ErrorResponse errorResponse = new ErrorResponse("Фильм с ID " + id + " не найден");
                    sendJson(ex, 404, new Gson().toJson(errorResponse));
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный ID. ID должен быть цифрой. Вы ввели: "
                        + endpoint.split("/")[2]);
                sendJson(ex, 400, new Gson().toJson(errorResponse));
            }

        }
    }

    private static List<String> getInputErrors(String title, int year) {
        List<String> inputErrors = new ArrayList<>();
        if (title == null || title.isEmpty()) {
            inputErrors.add("Название не должно быть пустым");
        }

        if (title.length() > 100) {
            inputErrors.add("Название должно быть меньше 100 символов");
        }

        if (!(1888 <= year && year <= 2026)) {
            inputErrors.add("Год должен быть между 1888 и 2026");
        }
        return inputErrors;
    }
}