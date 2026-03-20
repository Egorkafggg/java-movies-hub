package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {

    private final HttpServer server;
    private final int port;
    private final MoviesStore moviesStore;

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            this.port = port;
            this.moviesStore = moviesStore;
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при создании HTTP-сервера", e);
        }
    }

    public MoviesStore getMoviesStore() {
        return moviesStore;
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(1);
        System.out.println("Сервер остановлен");
    }

    public void clearMovieStore() {
        moviesStore.clearStore();
    }
}