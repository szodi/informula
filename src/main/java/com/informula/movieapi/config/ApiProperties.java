package com.informula.movieapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "movie.api")
public record ApiProperties(ApiConfig omdb, ApiConfig tmdb) {

    public record ApiConfig(String baseUrl, String apiKey) {}
}
