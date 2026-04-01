package com.informula.movieapi.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbSearchResponse(
        List<TmdbMovieResult> results,
        @JsonProperty("total_results") Integer totalResults,
        @JsonProperty("total_pages") Integer totalPages
) {}
