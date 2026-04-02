package com.informula.movieapi.service;

import com.informula.movieapi.entity.SearchHistory;
import com.informula.movieapi.enums.ApiName;
import com.informula.movieapi.repository.SearchHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {

    @Mock
    private SearchHistoryRepository repository;

    @InjectMocks
    private SearchHistoryService service;

    @Test
    void saveAsync_persistsNormalisedEntry() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.saveAsync("Avengers ", ApiName.OMDB, 5);

        ArgumentCaptor<SearchHistory> captor = ArgumentCaptor.forClass(SearchHistory.class);
        verify(repository).save(captor.capture());

        SearchHistory saved = captor.getValue();
        assertThat(saved.getQuery()).isEqualTo("avengers");
        assertThat(saved.getApiName()).isEqualTo(ApiName.OMDB);
        assertThat(saved.getResultCount()).isEqualTo(5);
        assertThat(saved.getSearchedAt()).isNotNull();
    }

    @Test
    void saveAsync_doesNotThrow_whenRepositoryFails() {
        doThrow(new RuntimeException("DB unavailable")).when(repository).save(any());

        assertThatNoException().isThrownBy(() -> service.saveAsync("Thor", ApiName.TMDB, 3));
    }
}
