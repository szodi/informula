package com.informula.movieapi.service;

import com.informula.movieapi.entity.SearchHistory;
import com.informula.movieapi.enums.ApiName;
import com.informula.movieapi.repository.SearchHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SearchHistoryService {

    private static final Logger log = LoggerFactory.getLogger(SearchHistoryService.class);

    private final SearchHistoryRepository repository;

    public SearchHistoryService(SearchHistoryRepository repository) {
        this.repository = repository;
    }

    @Async("asyncTaskExecutor")
    @Transactional
    public void saveAsync(String query, ApiName apiName, int resultCount) {
        try {
            repository.save(new SearchHistory(
                    query.trim().toLowerCase(),
                    apiName,
                    resultCount,
                    LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Failed to persist search history for query='{}', api='{}': {}",
                    query, apiName, e.getMessage());
        }
    }
}
