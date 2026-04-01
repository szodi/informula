package com.informula.movieapi.controller;

import com.informula.movieapi.dto.MovieDto;
import com.informula.movieapi.dto.MovieResponse;
import com.informula.movieapi.exception.InvalidApiException;
import com.informula.movieapi.service.MovieService;
import com.informula.movieapi.service.SearchHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private SearchHistoryService searchHistoryService;

    @MockitoBean
    private CacheManager cacheManager;

    @Test
    void searchMovies_returnsMovieList_whenResultsFound() throws Exception {
        MovieResponse response = new MovieResponse(List.of(
                new MovieDto("Avengers: Endgame", "2019", List.of("Anthony Russo", "Joe Russo"))
        ));

        when(movieService.searchMovies("Avengers", "omdb")).thenReturn(response);

        mockMvc.perform(get("/movies/Avengers")
                        .param("api", "omdb")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.movies", hasSize(1)))
                .andExpect(jsonPath("$.movies[0].Title", is("Avengers: Endgame")))
                .andExpect(jsonPath("$.movies[0].Year", is("2019")))
                .andExpect(jsonPath("$.movies[0].Director", hasItems("Anthony Russo", "Joe Russo")));

        verify(searchHistoryService).saveAsync("Avengers", "omdb", 1);
    }

    @Test
    void searchMovies_returnsEmptyList_whenNoResultsFound() throws Exception {
        when(movieService.searchMovies("XYZ123Unknown", "tmdb"))
                .thenReturn(new MovieResponse(List.of()));

        mockMvc.perform(get("/movies/XYZ123Unknown")
                        .param("api", "tmdb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies", hasSize(0)));
    }

    @Test
    void searchMovies_returns400_whenApiParamMissing() throws Exception {
        mockMvc.perform(get("/movies/Avengers"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void searchMovies_returns400_whenApiNameInvalid() throws Exception {
        when(movieService.searchMovies(anyString(), eq("invalid")))
                .thenThrow(new InvalidApiException("Unknown API 'invalid'. Supported values: omdb, tmdb"));

        mockMvc.perform(get("/movies/Avengers")
                        .param("api", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unknown API")));
    }

    @Test
    void searchMovies_doesNotBlockOnHistorySave() throws Exception {
        when(movieService.searchMovies("Inception", "omdb"))
                .thenReturn(new MovieResponse(List.of()));

        doNothing().when(searchHistoryService).saveAsync(anyString(), anyString(), anyInt());

        mockMvc.perform(get("/movies/Inception").param("api", "omdb"))
                .andExpect(status().isOk());

        verify(searchHistoryService, times(1)).saveAsync("Inception", "omdb", 0);
    }
}
