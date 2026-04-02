package com.informula.movieapi.service;

import com.informula.movieapi.dto.MovieResponse;
import com.informula.movieapi.enums.ApiName;
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

    @Cacheable(value = "movies", key = "#api.beanName() + ':' + #title.toLowerCase().trim()")
    public MovieResponse searchMovies(String title, ApiName api) {
        log.info("Cache miss — fetching from {} for title='{}'", api.beanName(), title);
        return new MovieResponse(movieApiServices.get(api.beanName()).searchMovies(title));
    }
}
