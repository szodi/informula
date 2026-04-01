package com.informula.movieapi.dto.omdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchResponse(
        @JsonProperty("Search") List<OmdbSearchResult> search,
        @JsonProperty("totalResults") String totalResults,
        @JsonProperty("Response") String response,
        @JsonProperty("Error") String error
) {}
