package com.informula.movieapi.service.impl;

import com.informula.movieapi.config.ApiProperties;
import com.informula.movieapi.dto.MovieDto;
import com.informula.movieapi.dto.tmdb.TmdbCreditsResponse;
import com.informula.movieapi.dto.tmdb.TmdbCrewMember;
import com.informula.movieapi.dto.tmdb.TmdbMovieResult;
import com.informula.movieapi.dto.tmdb.TmdbSearchResponse;
import com.informula.movieapi.service.MovieApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service("tmdb")
public class TmdbApiService implements MovieApiService {

    private static final Logger log = LoggerFactory.getLogger(TmdbApiService.class);
    private static final int PARALLEL_CREDITS_CALLS = 10;

    private final WebClient webClient;
    private final String apiKey;

    public TmdbApiService(
            @Qualifier("tmdbWebClient") WebClient webClient,
            ApiProperties apiProperties
    ) {
        this.webClient = webClient;
        this.apiKey = apiProperties.tmdb().apiKey();
    }

    @Override
    public List<MovieDto> searchMovies(String title) {
        log.debug("TMDB search: title='{}'", title);

        List<MovieDto> results = webClient.get()
                .uri(uri -> uri
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", title)
                        .queryParam("include_adult", true)
                        .build())
                .retrieve()
                .bodyToMono(TmdbSearchResponse.class)
                .flatMapMany(response -> {
                    if (response.results() == null || response.results().isEmpty()) {
                        log.debug("TMDB returned no results for '{}'", title);
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.results())
                            .flatMap(this::fetchWithDirectors, PARALLEL_CREDITS_CALLS);
                })
                .collectList()
                .onErrorResume(ex -> {
                    log.error("TMDB search failed for '{}': {}", title, ex.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();

        return results != null ? results : Collections.emptyList();
    }

    private Mono<MovieDto> fetchWithDirectors(TmdbMovieResult movie) {
        return webClient.get()
                .uri(uri -> uri
                        .path("/movie/{id}/credits")
                        .queryParam("api_key", apiKey)
                        .build(movie.id()))
                .retrieve()
                .bodyToMono(TmdbCreditsResponse.class)
                .map(credits -> {
                    List<String> directors = Collections.emptyList();
                    if (credits.crew() != null) {
                        directors = credits.crew().stream()
                                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                                .map(TmdbCrewMember::name)
                                .collect(Collectors.toList());
                    }
                    return buildDto(movie, directors);
                })
                .onErrorResume(ex -> {
                    log.warn("TMDB credits call failed for movieId={}: {}", movie.id(), ex.getMessage());
                    return Mono.just(buildDto(movie, Collections.emptyList()));
                });
    }

    private MovieDto buildDto(TmdbMovieResult movie, List<String> directors) {
        return new MovieDto(movie.title(), extractYear(movie.releaseDate()), directors);
    }

    private String extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) {
            return "N/A";
        }
        return releaseDate.substring(0, 4);
    }
}
