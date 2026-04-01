package com.informula.movieapi.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieResult(
        Long id,
        String title,
        @JsonProperty("release_date") String releaseDate
) {}
