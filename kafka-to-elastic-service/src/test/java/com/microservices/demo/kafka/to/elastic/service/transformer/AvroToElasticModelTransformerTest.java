package com.microservices.demo.kafka.to.elastic.service.transformer;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AvroToElasticModelTransformerTest {

    private final AvroToElasticModelTransformer transformer = new AvroToElasticModelTransformer();

    @Test
    void getElasticModels_mapsEveryFieldFromTheAvroContract() {
        long createdAtEpochMilli = Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();
        TwitterAvroModel avroModel = TwitterAvroModel.newBuilder()
                .setId(42L)
                .setUserId(7L)
                .setText("hello world")
                .setCreatedAt(createdAtEpochMilli)
                .build();

        List<TwitterIndexModel> result = transformer.getElasticModels(List.of(avroModel));

        assertThat(result).hasSize(1);
        TwitterIndexModel indexModel = result.get(0);
        assertThat(indexModel.getId()).isEqualTo("42");
        assertThat(indexModel.getUserId()).isEqualTo(7L);
        assertThat(indexModel.getText()).isEqualTo("hello world");
        assertThat(indexModel.getCreatedAt())
                .isEqualTo(ZonedDateTime.ofInstant(Instant.ofEpochMilli(createdAtEpochMilli), ZoneId.systemDefault()));
    }

    @Test
    void getElasticModels_mapsMultipleMessagesInOrder() {
        TwitterAvroModel first = TwitterAvroModel.newBuilder()
                .setId(1L).setUserId(1L).setText("first").setCreatedAt(0L).build();
        TwitterAvroModel second = TwitterAvroModel.newBuilder()
                .setId(2L).setUserId(2L).setText("second").setCreatedAt(0L).build();

        List<TwitterIndexModel> result = transformer.getElasticModels(List.of(first, second));

        assertThat(result).extracting(TwitterIndexModel::getId).containsExactly("1", "2");
        assertThat(result).extracting(TwitterIndexModel::getText).containsExactly("first", "second");
    }
}
