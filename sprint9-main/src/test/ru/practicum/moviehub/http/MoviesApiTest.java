package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        server.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    //GET
    @Test
    void testGetEmptyListFromEmptyStore() throws Exception {
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaders = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaders,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
        assertEquals("[]", body, "Ожидается пустой массив");

    }

    @Test
    void testGetListOf3Movies() throws Exception {
        Movie movie1 = new Movie("Movie1", 1900);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        Gson gson = new Gson();
        ListOfMoviesTypeToken listOfMoviesTypeToken = new ListOfMoviesTypeToken();
        List<Movie> movies = gson.fromJson(body, listOfMoviesTypeToken.getType());
        assertEquals(3, movies.size());
        assertTrue(movies.get(0).equals(movie1) && movies.get(2).equals(movie3));

    }

    @Test
    void testGetMovieById() throws Exception {
        Movie movie1 = new Movie("Movie1", 1900);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();
        assertEquals(200, resp.statusCode());
        assertEquals("{\"title\":\"Movie1\",\"year\":1900}", body);
    }

    @Test
    void testGetMovieById_Error404_NotExistID() throws Exception {
        Movie movie1 = new Movie("Movie1", 1900);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/4"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();
        assertEquals(404, resp.statusCode());
        assertEquals("{\"description\":\"Фильм с ID 4 не найден\"}", body);
    }

    @Test
    void testGetMovieById_Error400_IdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/s"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();
        assertEquals(400, resp.statusCode());
        assertEquals("{\"description\":\"Некорректный ID. ID должен быть цифрой. Вы ввели: s\"}", body);
    }

    @Test
    void testGetMoviesByYear() throws Exception {
        Movie movie1 = new Movie("Movie1", 2010);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies?year=2010"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(200, resp.statusCode());
        String body = resp.body();
        Gson gson = new Gson();
        ListOfMoviesTypeToken listOfMoviesTypeToken = new ListOfMoviesTypeToken();
        List<Movie> movies = gson.fromJson(body, listOfMoviesTypeToken.getType());
        assertEquals(2, movies.size());
        assertTrue(movies.get(0).getYear() == 2010 && movies.get(1).getYear() == 2010);
    }

    @Test
    void testGetMoviesByYear_EmptyList() throws Exception {
        Movie movie1 = new Movie("Movie1", 2010);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies?year=2011"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(200, resp.statusCode());
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"));
        assertEquals("[]", body, "Ожидается пустой массив");
    }

    @Test
    void getMoviesByYear_Error400_YearNotNumber() throws Exception {
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies?year=x"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(400, resp.statusCode());
        String body = resp.body();
        assertEquals("{\"description\":\"Некорректный параметр запроса — year\"}", body);

    }

    //POST
    @Test
    void testPostMovies() throws Exception {
        String jsonBody = "{\"title\":\"Movie4\",\"year\":\"1900\"}";
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        String headerValue = resp.headers().firstValue("Content-Type").orElse("");
        String body = resp.body();

        assertEquals(201, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", headerValue);
        assertEquals("{\"title\":\"Movie4\",\"year\":1900}", body);
    }

    @Test
    void testPostMovies_Error422_EmptyTitle() throws Exception {
        String jsonBody = "{\"title\":\"\",\"year\":\"1900\"}";
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String headerValue = resp.headers().firstValue("Content-Type").orElse("");
        String body = resp.body();

        assertEquals(422, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", headerValue);
        assertEquals("{\"description\":\"Ошибка ввода\",\"value\":[\"Название не должно быть пустым\"]}", body);
    }

    @Test
    void testPostMovies_Error422_LongTitle() throws Exception {
        String longTitle = "X".repeat(101);
        String jsonBody = String.format("{\"title\":\"%s\",\"year\":\"1999\"}", longTitle);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();

        assertEquals(422, resp.statusCode());
        assertEquals("{\"description\":\"Ошибка ввода\",\"value\":[\"Название должно быть меньше 100 символов\"]}",
                body);
    }

    @Test
    void testPostMovies_Error422_WrongYear() throws Exception {
        String jsonBody = "{\"title\":\"Movie1\",\"year\":\"1887\"}";
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();

        assertEquals(422, resp.statusCode());
        assertEquals("{\"description\":\"Ошибка ввода\",\"value\":[\"Год должен быть между 1888 и 2026\"]}",
                body);
    }

    @Test
    void testPostMovies_Error415_WrongContentType() throws Exception {
        String jsonBody = "{\"title\":\"Movie1\",\"year\":\"1888\"}";
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(415, resp.statusCode());
    }

    @Test
    void testPostMovies_Error400_WrongJSON() throws Exception {
        String jsonBody = "{\"title\":\"Movie1\",\"year\":\"x\"}";
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        String body = resp.body();

        assertEquals(400, resp.statusCode());
        assertEquals("{\"description\":\"Некорректное значение года\"}", body);
    }

    //DELETE
    @Test
    void testDeleteMovieById() throws Exception {
        Movie movie1 = new Movie("Movie1", 1900);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(204, resp.statusCode());
        assertEquals(2, server.getMoviesStore().getAllMovies().size());
        assertTrue(server.getMoviesStore().getAllMovies().get(0).equals(movie1)
                && server.getMoviesStore().getAllMovies().get(1).equals(movie3));
    }

    @Test
    void testDeleteMovieById_Error404_NotExistId() throws Exception {
        Movie movie1 = new Movie("Movie1", 1900);
        Movie movie2 = new Movie("Movie2", 1950);
        Movie movie3 = new Movie("Movie3", 2010);
        server.getMoviesStore().addMovie(movie1);
        server.getMoviesStore().addMovie(movie2);
        server.getMoviesStore().addMovie(movie3);

        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/10"))
                .DELETE()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(404, resp.statusCode());
        String body = resp.body();
        assertEquals("{\"description\":\"Фильм с ID 10 не найден\"}", body);
    }

    @Test
    void deleteMovieById_Error400_IdIsNotNumber() throws Exception {
        HttpRequest req = HttpRequest
                .newBuilder()
                .uri(URI.create(BASE + "/movies/x"))
                .DELETE()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(400, resp.statusCode());
        String body = resp.body();
        assertEquals("{\"description\":\"Некорректный ID. ID должен быть цифрой. Вы ввели: x\"}", body);
    }
}