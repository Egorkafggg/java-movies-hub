package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CONTENT_TYPE = "application/json; charset=UTF-8";
    protected static final Gson gson = new GsonBuilder().create();

    protected void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendResponse(HttpExchange exchange, int statusCode, Object body) throws IOException {
        sendResponse(exchange, statusCode, gson.toJson(body));
    }

    protected void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        exchange.sendResponseHeaders(204, -1);
    }

    protected void sendError(HttpExchange exchange, int statusCode, String error) throws IOException {
        sendResponse(exchange, statusCode, new ErrorResponse(error));
    }

    protected void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendError(exchange, 405, "Метод не поддерживается");
    }

    protected void sendUnsupportedMediaType(HttpExchange exchange) throws IOException {
        sendError(exchange, 415, "Неподдерживаемый тип контента");
    }

    protected String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected boolean isJsonContentType(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }
}