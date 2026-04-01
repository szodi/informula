package com.informula.movieapi.service.impl;

import com.informula.movieapi.config.ApiProperties;
import com.informula.movieapi.dto.MovieDto;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TmdbApiServiceTest {

    private MockWebServer mockWebServer;
    private TmdbApiService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        ApiProperties props = new ApiProperties(
                new ApiProperties.ApiConfig("", ""),
                new ApiProperties.ApiConfig(mockWebServer.url("/").toString(), "test-tmdb-key"));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        service = new TmdbApiService(webClient, props);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void searchMovies_returnsParsedMovies_withDirectors() throws InterruptedException {
        mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath();
                if (path.contains("/search/movie")) {
                    return new MockResponse()
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody("""
                                    {
                                      "results": [
                                        {"id": 24428, "title": "The Avengers", "release_date": "2012-05-04"},
                                        {"id": 299536, "title": "Avengers: Infinity War", "release_date": "2018-04-27"}
                                      ]
                                    }
                                    """);
                } else if (path.contains("/movie/24428/credits")) {
                    return new MockResponse()
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody("""
                                    {
                                      "id": 24428,
                                      "crew": [
                                        {"id": 1, "name": "Joss Whedon", "job": "Director", "department": "Directing"},
                                        {"id": 2, "name": "Kevin Feige", "job": "Producer", "department": "Production"}
                                      ]
                                    }
                                    """);
                } else if (path.contains("/movie/299536/credits")) {
                    return new MockResponse()
                            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .setBody("""
                                    {
                                      "id": 299536,
                                      "crew": [
                                        {"id": 3, "name": "Anthony Russo", "job": "Director", "department": "Directing"},
                                        {"id": 4, "name": "Joe Russo", "job": "Director", "department": "Directing"}
                                      ]
                                    }
                                    """);
                }
                return new MockResponse().setResponseCode(404);
            }
        });

        List<MovieDto> movies = service.searchMovies("Avengers");

        assertThat(movies).hasSize(2);

        MovieDto avengers = movies.stream()
                .filter(m -> "The Avengers".equals(m.title()))
                .findFirst().orElseThrow();
        assertThat(avengers.year()).isEqualTo("2012");
        assertThat(avengers.director()).containsExactly("Joss Whedon");

        MovieDto infinityWar = movies.stream()
                .filter(m -> m.title().contains("Infinity War"))
                .findFirst().orElseThrow();
        assertThat(infinityWar.director()).containsExactlyInAnyOrder("Anthony Russo", "Joe Russo");
    }

    @Test
    void searchMovies_returnsEmptyList_whenNoResults() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"results": [], "total_results": 0}
                        """));

        List<MovieDto> movies = service.searchMovies("NoSuchFilm99999");

        assertThat(movies).isEmpty();
    }

    @Test
    void searchMovies_includesMovieWithEmptyDirectors_whenCreditsFail() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "results": [{"id": 1, "title": "Some Film", "release_date": "2020-01-01"}]
                        }
                        """));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        List<MovieDto> movies = service.searchMovies("Some Film");

        assertThat(movies).hasSize(1);
        assertThat(movies.get(0).title()).isEqualTo("Some Film");
        assertThat(movies.get(0).year()).isEqualTo("2020");
        assertThat(movies.get(0).director()).isEmpty();
    }

    @Test
    void extractYear_handlesShortOrNullReleaseDate() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "results": [{"id": 2, "title": "TBD Film", "release_date": ""}]
                        }
                        """));
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"id": 2, "crew": []}
                        """));

        List<MovieDto> movies = service.searchMovies("TBD Film");

        assertThat(movies).hasSize(1);
        assertThat(movies.get(0).year()).isEqualTo("N/A");
    }
}
