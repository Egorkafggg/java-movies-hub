package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private MoviesStore moviesStore;

    public MoviesServer(MoviesStore store, int port) {
        try {
            // создайте сервер
            moviesStore = store;
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/movies", new MoviesHandlerMovies(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        // запустите сервер
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public void clear() {
        moviesStore.clearMovies();
    }
}