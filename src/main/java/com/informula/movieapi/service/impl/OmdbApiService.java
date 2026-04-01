package com.informula.movieapi.service.impl;

import com.informula.movieapi.config.ApiProperties;
import com.informula.movieapi.dto.MovieDto;
import com.informula.movieapi.dto.omdb.OmdbDetailResponse;
import com.informula.movieapi.dto.omdb.OmdbSearchResponse;
import com.informula.movieapi.service.MovieApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service("omdb")
public class OmdbApiService implements MovieApiService {

    private static final Logger log = LoggerFactory.getLogger(OmdbApiService.class);
    private static final int PARALLEL_DETAIL_CALLS = 10;
    private static final String OMDB_TRUE = "True";

    private final WebClient webClient;
    private final String apiKey;

    public OmdbApiService(@Qualifier("omdbWebClient") WebClient webClient,
                          ApiProperties apiProperties) {
        this.webClient = webClient;
        this.apiKey = apiProperties.omdb().apiKey();
    }

    @Override
    public List<MovieDto> searchMovies(String title) {
        log.debug("OMDB search: title='{}'", title);

        List<MovieDto> results = webClient.get()
                .uri(uri -> uri
                        .queryParam("s", title)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(OmdbSearchResponse.class)
                .flatMapMany(response -> {
                    if (!OMDB_TRUE.equalsIgnoreCase(response.response())
                            || response.search() == null) {
                        log.debug("OMDB returned no results for '{}': {}", title, response.error());
                        return Flux.empty();
                    }
                    return Flux.fromIterable(response.search())
                            .flatMap(item -> fetchDetail(item.imdbId()), PARALLEL_DETAIL_CALLS);
                })
                .collectList()
                .onErrorResume(ex -> {
                    log.error("OMDB search failed for '{}': {}", title, ex.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();

        return results != null ? results : Collections.emptyList();
    }

    private Mono<MovieDto> fetchDetail(String imdbId) {
        return webClient.get()
                .uri(uri -> uri
                        .queryParam("i", imdbId)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(OmdbDetailResponse.class)
                .mapNotNull(detail -> {
                    if (!OMDB_TRUE.equalsIgnoreCase(detail.response())) {
                        log.debug("OMDB detail returned false for imdbId={}", imdbId);
                        return null;
                    }
                    return new MovieDto(detail.title(), detail.year(), parseDirectors(detail.director()));
                })
                .onErrorResume(ex -> {
                    log.warn("OMDB detail call failed for imdbId={}: {}", imdbId, ex.getMessage());
                    return Mono.empty();
                });
    }

    private List<String> parseDirectors(String raw) {
        if (raw == null || raw.isBlank() || "N/A".equalsIgnoreCase(raw.trim())) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
