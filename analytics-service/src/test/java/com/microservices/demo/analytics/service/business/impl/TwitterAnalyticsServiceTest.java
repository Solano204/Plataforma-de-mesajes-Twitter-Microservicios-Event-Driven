package com.microservices.demo.analytics.service.business.impl;

import com.microservices.demo.analytics.service.dataaccess.entity.AnalyticsEntity;
import com.microservices.demo.analytics.service.dataaccess.repository.AnalyticsRepository;
import com.microservices.demo.analytics.service.model.AnalyticsResponseModel;
import com.microservices.demo.analytics.service.transformer.EntityToResponseModelTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterAnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @Mock
    private EntityToResponseModelTransformer entityToResponseModelTransformer;

    private TwitterAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new TwitterAnalyticsService(analyticsRepository, entityToResponseModelTransformer);
    }

    @Test
    void getWordAnalytics_returnsTransformedMostRecentEntity() {
        AnalyticsEntity mostRecent = new AnalyticsEntity(UUID.randomUUID(), "hello", 9L, LocalDateTime.now());
        AnalyticsResponseModel expected = AnalyticsResponseModel.builder().word("hello").wordCount(9L).build();
        when(analyticsRepository.getAnalyticsEntitiesByWord(eq("hello"), any()))
                .thenReturn(List.of(mostRecent));
        when(entityToResponseModelTransformer.getResponseModel(mostRecent))
                .thenReturn(Optional.of(expected));

        Optional<AnalyticsResponseModel> result = service.getWordAnalytics("hello");

        assertThat(result).contains(expected);
    }

    @Test
    void getWordAnalytics_returnsEmpty_whenNoEntitiesFound() {
        when(analyticsRepository.getAnalyticsEntitiesByWord(eq("missing"), any()))
                .thenReturn(List.of());
        when(entityToResponseModelTransformer.getResponseModel(null))
                .thenReturn(Optional.empty());

        Optional<AnalyticsResponseModel> result = service.getWordAnalytics("missing");

        assertThat(result).isEmpty();
    }
}
