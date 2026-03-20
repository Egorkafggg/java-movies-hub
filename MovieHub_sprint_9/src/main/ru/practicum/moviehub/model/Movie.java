package ru.practicum.moviehub.model;

public class Movie {
    private Long id;
    private final String title;
    private final Integer year;

    public Movie() {
        this.title = null;
        this.year = null;
    }

    public Movie(String title, Integer year) {
        this.title = title;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }
}