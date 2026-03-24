package ru.practicum.moviehub;

import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;
import java.util.logging.Logger;

public class MovieHubApp {
    private static final Logger logger = Logger.getLogger(MovieHubApp.class.getName());
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = parsePortFromArgs(args);
        MoviesServer server = null;

        try {
            server = new MoviesServer(new MoviesStore(), port);
            Runtime.getRuntime().addShutdownHook(createShutdownHook(server));
            logger.info("Starting MovieHub server on port " + port);
            server.start();
            logger.info("MovieHub server started successfully");
        } catch (Exception e) {
            logger.severe("Failed to start MovieHub server: " + e.getMessage());
            if (server != null) {
                server.stop();
            }
            System.exit(1);
        }
    }

    private static int parsePortFromArgs(String[] args) {
        if (args != null && args.length > 0) {
            try {
                int port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    logger.warning("Invalid port number: " + port + ". Using default port " + DEFAULT_PORT);
                    return DEFAULT_PORT;
                }
                return port;
            } catch (NumberFormatException e) {
                logger.warning("Invalid port argument: " + args[0] + ". Using default port " + DEFAULT_PORT);
                return DEFAULT_PORT;
            }
        }
        return DEFAULT_PORT;
    }

    private static Thread createShutdownHook(MoviesServer server) {
        return new Thread(() -> {
            if (server != null) {
                try {
                    logger.info("Shutting down MovieHub server...");
                    server.stop();
                    logger.info("MovieHub server stopped successfully");
                } catch (Exception e) {
                    logger.severe("Error during server shutdown: " + e.getMessage());
                }
            }
        });
    }
}