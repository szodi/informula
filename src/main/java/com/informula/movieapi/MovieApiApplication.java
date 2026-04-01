package com.informula.movieapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@ConfigurationPropertiesScan
public class MovieApiApplication {

    static void main(String[] args) {
        SpringApplication.run(MovieApiApplication.class, args);
    }
}
