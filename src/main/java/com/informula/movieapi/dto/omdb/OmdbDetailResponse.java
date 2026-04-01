package com.informula.movieapi.dto.omdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbDetailResponse(
        @JsonProperty("Title") String title,
        @JsonProperty("Year") String year,
        @JsonProperty("Director") String director,
        @JsonProperty("Response") String response,
        @JsonProperty("Error") String error
) {}
