package com.informula.movieapi.controller;

import com.informula.movieapi.dto.MovieResponse;
import com.informula.movieapi.service.MovieService;
import com.informula.movieapi.service.SearchHistoryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/movies")
public class MovieController {

    private final MovieService movieService;
    private final SearchHistoryService searchHistoryService;

    public MovieController(MovieService movieService, SearchHistoryService searchHistoryService) {
        this.movieService = movieService;
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping(value = "/{movieTitle}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MovieResponse> searchMovies(
            @PathVariable @NotBlank(message = "Movie title must not be blank") String movieTitle,
            @RequestParam @NotBlank(message = "API name must not be blank") String api) {

        MovieResponse response = movieService.searchMovies(movieTitle, api);
        searchHistoryService.saveAsync(movieTitle, api, response.movies().size());
        return ResponseEntity.ok(response);
    }
}
