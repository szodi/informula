package com.informula.movieapi.repository;

import com.informula.movieapi.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    /** Top searched queries for a given API — useful for pre-warming the cache. */
    @Query("""
            SELECT s.query, COUNT(s) AS cnt
            FROM SearchHistory s
            WHERE s.apiName = :apiName
            GROUP BY s.query
            ORDER BY cnt DESC
            LIMIT :limit
            """)
    List<Object[]> findTopQueriesByApi(String apiName, int limit);
}
