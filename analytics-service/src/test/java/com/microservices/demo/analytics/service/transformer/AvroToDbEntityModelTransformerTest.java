package com.microservices.demo.analytics.service.transformer;

import com.microservices.demo.analytics.service.dataaccess.entity.AnalyticsEntity;
import com.microservices.demo.kafka.avro.model.TwitterAnalyticsAvroModel;
import org.junit.jupiter.api.Test;
import org.springframework.util.IdGenerator;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AvroToDbEntityModelTransformerTest {

    @Test
    void getEntityModel_mapsEveryFieldFromTheAvroContract() {
        UUID generatedId = UUID.randomUUID();
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.generateId()).thenReturn(generatedId);
        AvroToDbEntityModelTransformer transformer = new AvroToDbEntityModelTransformer(idGenerator);

        long createdAtEpochSecond = Instant.parse("2026-01-01T00:00:00Z").getEpochSecond();
        TwitterAnalyticsAvroModel avroModel = TwitterAnalyticsAvroModel.newBuilder()
                .setWord("hello")
                .setWordCount(3L)
                .setCreatedAt(createdAtEpochSecond)
                .build();

        List<AnalyticsEntity> result = transformer.getEntityModel(List.of(avroModel));

        assertThat(result).hasSize(1);
        AnalyticsEntity entity = result.get(0);
        assertThat(entity.getId()).isEqualTo(generatedId);
        assertThat(entity.getWord()).isEqualTo("hello");
        assertThat(entity.getWordCount()).isEqualTo(3L);
        assertThat(entity.getRecordDate())
                .isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochSecond(createdAtEpochSecond), ZoneOffset.UTC));
    }
}
