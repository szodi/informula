package com.informula.movieapi.service;

import com.informula.movieapi.dto.MovieDto;

import java.util.List;

/**
 * Strategy interface for external movie data providers.
 *
 * Each implementation is registered as a Spring bean whose name matches the
 * lowercase API identifier used in the query parameter (e.g. "omdb", "tmdb").
 * {@link MovieService} resolves the correct implementation at runtime via
 * {@code Map<String, MovieApiService>} injection — no if/else chains needed.
 */
public interface MovieApiService {

    /**
     * Search for movies matching {@code title} and return their basic metadata.
     *
     * @param title the (possibly partial) movie title to search for
     * @return list of matching movies; never {@code null}, may be empty
     */
    List<MovieDto> searchMovies(String title);
}
