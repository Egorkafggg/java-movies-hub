package ru.practicum.moviehub.api;

public class ErrorResponse {
    private String description;
    private Object value;

    public ErrorResponse(String error) {
        this.description = error;
    }


    public ErrorResponse(String error, Object details) {
        this.description = error;
        this.value = details;
    }

}