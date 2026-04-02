package com.informula.movieapi.enums;

import com.informula.movieapi.exception.InvalidApiException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum ApiName {
    OMDB("omdb"),
    TMDB("tmdb");

    private final String beanName;

    ApiName(String beanName) {
        this.beanName = beanName;
    }

    public String beanName() {
        return beanName;
    }

    public static ApiName fromString(String value) {
        return Arrays.stream(values())
                .filter(a -> a.beanName.equalsIgnoreCase(value) || a.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new InvalidApiException(
                        "Unknown API '" + value + "'. Supported values: " +
                                Arrays.stream(values()).map(ApiName::beanName).collect(Collectors.joining(", "))));
    }
}
