package com.informula.movieapi.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbCrewMember(
        Long id,
        String name,
        String job,
        String department
) {}
