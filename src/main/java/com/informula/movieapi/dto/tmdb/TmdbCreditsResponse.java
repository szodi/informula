package com.informula.movieapi.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbCreditsResponse(
        Long id,
        List<TmdbCrewMember> crew
) {}
