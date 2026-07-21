package com.microservices.demo.analytics.service.transformer;

import com.microservices.demo.analytics.service.dataaccess.entity.AnalyticsEntity;
import com.microservices.demo.analytics.service.model.AnalyticsResponseModel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityToResponseModelTransformerTest {

    private final EntityToResponseModelTransformer transformer = new EntityToResponseModelTransformer();

    @Test
    void getResponseModel_mapsEntityFields() {
        UUID id = UUID.randomUUID();
        AnalyticsEntity entity = new AnalyticsEntity(id, "hello", 5L, LocalDateTime.now());

        Optional<AnalyticsResponseModel> result = transformer.getResponseModel(entity);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
        assertThat(result.get().getWord()).isEqualTo("hello");
        assertThat(result.get().getWordCount()).isEqualTo(5L);
    }

    @Test
    void getResponseModel_returnsEmpty_whenEntityIsNull() {
        Optional<AnalyticsResponseModel> result = transformer.getResponseModel(null);

        assertThat(result).isEmpty();
    }
}
