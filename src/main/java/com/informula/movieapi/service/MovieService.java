package com.informula.movieapi.service;

import com.informula.movieapi.dto.MovieResponse;
import com.informula.movieapi.exception.InvalidApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    private final Map<String, MovieApiService> movieApiServices;

    public MovieService(Map<String, MovieApiService> movieApiServices) {
        this.movieApiServices = movieApiServices;
    }

    @Cacheable(value = "movies", key = "#apiName.toLowerCase() + ':' + #title.toLowerCase().trim()")
    public MovieResponse searchMovies(String title, String apiName) {
        String normalisedApi = apiName.toLowerCase();
        log.info("Cache miss — fetching from {} for title='{}'", normalisedApi, title);

        MovieApiService service = movieApiServices.get(normalisedApi);
        if (service == null) {
            throw new InvalidApiException("Unknown API '" + apiName + "'. Supported values: omdb, tmdb");
        }

        return new MovieResponse(service.searchMovies(title));
    }
}
