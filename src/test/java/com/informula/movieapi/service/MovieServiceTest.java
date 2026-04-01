package com.informula.movieapi.service;

import com.informula.movieapi.dto.MovieDto;
import com.informula.movieapi.dto.MovieResponse;
import com.informula.movieapi.exception.InvalidApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieApiService omdbService;

    @Mock
    private MovieApiService tmdbService;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(Map.of("omdb", omdbService, "tmdb", tmdbService));
    }

    @Test
    void searchMovies_routesToOmdbService_whenApiIsOmdb() {
        List<MovieDto> movies = List.of(new MovieDto("Iron Man", "2008", List.of("Jon Favreau")));
        when(omdbService.searchMovies("Iron Man")).thenReturn(movies);

        MovieResponse response = movieService.searchMovies("Iron Man", "omdb");

        assertThat(response.movies()).hasSize(1);
        assertThat(response.movies().get(0).title()).isEqualTo("Iron Man");
        verify(omdbService).searchMovies("Iron Man");
        verifyNoInteractions(tmdbService);
    }

    @Test
    void searchMovies_routesToTmdbService_whenApiIsTmdb() {
        when(tmdbService.searchMovies("Thor")).thenReturn(List.of());

        movieService.searchMovies("Thor", "tmdb");

        verify(tmdbService).searchMovies("Thor");
        verifyNoInteractions(omdbService);
    }

    @Test
    void searchMovies_isCaseInsensitiveForApiName() {
        when(omdbService.searchMovies("Thor")).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> movieService.searchMovies("Thor", "OMDB"));
        verify(omdbService).searchMovies("Thor");
    }

    @Test
    void searchMovies_throwsInvalidApiException_whenApiNameUnknown() {
        assertThatThrownBy(() -> movieService.searchMovies("Thor", "unknown"))
                .isInstanceOf(InvalidApiException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void searchMovies_returnsEmptyMovieList_whenServiceReturnsEmpty() {
        when(omdbService.searchMovies("XYZ")).thenReturn(List.of());

        MovieResponse response = movieService.searchMovies("XYZ", "omdb");

        assertThat(response.movies()).isEmpty();
    }
}
