package com.informula.movieapi.dto.omdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchResult(
        @JsonProperty("Title") String title,
        @JsonProperty("Year") String year,
        @JsonProperty("imdbID") String imdbId,
        @JsonProperty("Type") String type
) {}
