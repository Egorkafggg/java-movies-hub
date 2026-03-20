package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    private static final Gson gson = new GsonBuilder().create();

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    // GET /movies

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMovies_whenHasMovies_returnsListOfMovies() throws Exception {
        server.getStore().add(new Movie("Начало", 2010));
        server.getStore().add(new Movie("Матрица", 1999));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
    }

    // POST /movies

    @Test
    void postMovie_withValidData_createsMovie() throws Exception {
        String json = "{\"title\":\"Интерстеллар\",\"year\":2014}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertNotNull(movie.getId());
        assertEquals("Интерстеллар", movie.getTitle());
        assertEquals(2014, movie.getYear());
    }

    @Test
    void postMovie_withEmptyTitle_returnsValidationError() throws Exception {
        String json = "{\"title\":\"\",\"year\":2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    void postMovie_withTooLongTitle_returnsValidationError() throws Exception {
        String longTitle = "А".repeat(101);
        String json = "{\"title\":\"" + longTitle + "\",\"year\":2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно превышать 100 символов"));
    }

    @Test
    void postMovie_withInvalidYearTooLow_returnsValidationError() throws Exception {
        String json = "{\"title\":\"Старый фильм\",\"year\":1800}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().stream().anyMatch(d -> d.contains("год должен быть между")));
    }

    @Test
    void postMovie_withInvalidYearTooHigh_returnsValidationError() throws Exception {
        String json = "{\"title\":\"Будущий фильм\",\"year\":3000}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().stream().anyMatch(d -> d.contains("год должен быть между")));
    }

    @Test
    void postMovie_withWrongContentType_returnsUnsupportedMediaType() throws Exception {
        String json = "{\"title\":\"Тест\",\"year\":2020}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, response.statusCode());
    }

    @Test
    void postMovie_withInvalidJson_returnsError() throws Exception {
        String invalidJson = "{invalid json}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный JSON", error.getError());
    }

    // GET /movies/{id}

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        Movie added = server.getStore().add(new Movie("Аватар", 2009));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
        Movie movie = gson.fromJson(response.body(), Movie.class);
        assertEquals(added.getId(), movie.getId());
        assertEquals("Аватар", movie.getTitle());
        assertEquals(2009, movie.getYear());
    }

    @Test
    void getMovieById_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void getMovieById_withInvalidId_returnsBadRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    // DELETE /movies/{id}

    @Test
    void deleteMovie_whenExists_returnsNoContent() throws Exception {
        Movie added = server.getStore().add(new Movie("Титаник", 1997));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
        assertTrue(server.getStore().getById(added.getId()).isEmpty());
    }

    @Test
    void deleteMovie_whenNotExists_returnsNotFound() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/999"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Фильм не найден", error.getError());
    }

    @Test
    void deleteMovie_withInvalidId_returnsBadRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/abc"))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный ID", error.getError());
    }

    // GET /movies?year=YYYY

    @Test
    void getMoviesByYear_whenExists_returnsFilteredList() throws Exception {
        server.getStore().add(new Movie("Фильм 2020 - 1", 2020));
        server.getStore().add(new Movie("Фильм 2020 - 2", 2020));
        server.getStore().add(new Movie("Фильм 2019", 2019));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, movies.size());
        assertTrue(movies.stream().allMatch(m -> Integer.valueOf(2020).equals(m.getYear())));
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyList() throws Exception {
        server.getStore().add(new Movie("Фильм 2020", 2020));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=2019"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        List<Movie> movies = gson.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        assertTrue(movies.isEmpty());
    }

    @Test
    void getMoviesByYear_withInvalidYear_returnsBadRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies?year=abc"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Некорректный параметр запроса — 'year'", error.getError());
    }

    // Общие проверки

    @Test
    void allSuccessResponses_haveCorrectContentType() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(CONTENT_TYPE, response.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void unsupportedMethod_returnsMethodNotAllowed() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies"))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
        ErrorResponse error = gson.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Метод не поддерживается", error.getError());
    }

    @Test
    void unsupportedMethodOnMovieById_returnsMethodNotAllowed() throws Exception {
        Movie added = server.getStore().add(new Movie("Тест", 2020));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/movies/" + added.getId()))
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }
}