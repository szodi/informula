package com.informula.movieapi.service.impl;

import com.informula.movieapi.config.ApiProperties;
import com.informula.movieapi.dto.MovieDto;
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

class OmdbApiServiceTest {

    private MockWebServer mockWebServer;
    private OmdbApiService service;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        ApiProperties props = new ApiProperties(
                new ApiProperties.ApiConfig(mockWebServer.url("/").toString(), "test-key"),
                new ApiProperties.ApiConfig("", ""));

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        service = new OmdbApiService(webClient, props);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void searchMovies_returnsParsedMovies_whenResultsFound() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "Search": [
                            {"Title":"Avengers","Year":"2012","imdbID":"tt0848228","Type":"movie"},
                            {"Title":"Avengers: Endgame","Year":"2019","imdbID":"tt4154796","Type":"movie"}
                          ],
                          "totalResults":"2",
                          "Response":"True"
                        }
                        """));

        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"Title":"Avengers","Year":"2012","Director":"Joss Whedon","Response":"True"}
                        """));
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"Title":"Avengers: Endgame","Year":"2019","Director":"Anthony Russo, Joe Russo","Response":"True"}
                        """));

        List<MovieDto> movies = service.searchMovies("Avengers");

        assertThat(movies).hasSize(2);

        MovieDto endgame = movies.stream()
                .filter(m -> m.title().contains("Endgame"))
                .findFirst().orElseThrow();
        assertThat(endgame.year()).isEqualTo("2019");
        assertThat(endgame.director()).containsExactlyInAnyOrder("Anthony Russo", "Joe Russo");

        RecordedRequest searchRequest = mockWebServer.takeRequest();
        assertThat(searchRequest.getPath()).contains("s=Avengers").contains("apikey=test-key");
    }

    @Test
    void searchMovies_returnsEmptyList_whenOmdbRespondsWithFalse() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"Response":"False","Error":"Movie not found!"}
                        """));

        List<MovieDto> movies = service.searchMovies("NoSuchMovie12345");

        assertThat(movies).isEmpty();
    }

    @Test
    void searchMovies_returnsEmptyList_whenNADirector() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "Search": [{"Title":"Unknown","Year":"2000","imdbID":"tt0000001","Type":"movie"}],
                          "totalResults":"1",
                          "Response":"True"
                        }
                        """));
        mockWebServer.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {"Title":"Unknown","Year":"2000","Director":"N/A","Response":"True"}
                        """));

        List<MovieDto> movies = service.searchMovies("Unknown");

        assertThat(movies).hasSize(1);
        assertThat(movies.get(0).director()).isEmpty();
    }

    @Test
    void searchMovies_returnsEmptyList_whenNetworkFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        List<MovieDto> movies = service.searchMovies("Avengers");
        assertThat(movies).isEmpty();
    }
}
