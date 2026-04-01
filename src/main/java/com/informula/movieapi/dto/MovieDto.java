package com.informula.movieapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MovieDto(
        @JsonProperty("Title") String title,
        @JsonProperty("Year") String year,
        @JsonProperty("Director") List<String> director
) {}
