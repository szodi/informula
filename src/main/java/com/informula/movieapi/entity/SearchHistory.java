package com.informula.movieapi.entity;

import com.informula.movieapi.enums.ApiName;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "search_history",
    indexes = {
        @Index(name = "idx_query", columnList = "query"),
        @Index(name = "idx_api_name", columnList = "api_name"),
        @Index(name = "idx_searched_at", columnList = "searched_at")
    }
)
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_search_history")
    @SequenceGenerator(name = "seq_search_history", sequenceName = "seq_search_history", allocationSize = 10)
    private Long id;

    @Column(nullable = false)
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApiName apiName;

    @Column
    private Integer resultCount;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    protected SearchHistory() {}

    public SearchHistory(String query, ApiName apiName, Integer resultCount, LocalDateTime searchedAt) {
        this.query = query;
        this.apiName = apiName;
        this.resultCount = resultCount;
        this.searchedAt = searchedAt;
    }

    public Long getId() { return id; }
    public String getQuery() { return query; }
    public ApiName getApiName() { return apiName; }
    public Integer getResultCount() { return resultCount; }
    public LocalDateTime getSearchedAt() { return searchedAt; }
}
