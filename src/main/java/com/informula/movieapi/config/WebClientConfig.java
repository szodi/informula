package com.informula.movieapi.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    private static final int CONNECT_TIMEOUT_MS = 3_000;
    private static final int READ_TIMEOUT_SECONDS = 5;
    private static final int MAX_IN_MEMORY_MB = 2;

    @Bean("omdbWebClient")
    public WebClient omdbWebClient(ApiProperties apiProperties) {
        return buildWebClient(apiProperties.omdb().baseUrl(), "OMDB");
    }

    @Bean("tmdbWebClient")
    public WebClient tmdbWebClient(ApiProperties apiProperties) {
        return buildWebClient(apiProperties.tmdb().baseUrl(), "TMDB");
    }

    private WebClient buildWebClient(String baseUrl, String apiLabel) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(MAX_IN_MEMORY_MB * 1024 * 1024))
                .filter(logRequest(apiLabel))
                .build();
    }

    /**
     * Lightweight request logger. Uses DEBUG level to avoid noise in production
     * while remaining useful when diagnosing third-party API issues.
     */
    private ExchangeFilterFunction logRequest(String apiLabel) {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("[{}] {} {}", apiLabel, clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }
}
