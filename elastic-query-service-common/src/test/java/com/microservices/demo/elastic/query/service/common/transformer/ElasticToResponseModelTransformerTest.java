package com.microservices.demo.elastic.query.service.common.transformer;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Same pattern already established elsewhere in this monorepo for
// transformers (AvroToElasticModelTransformerTest, EntityToResponseModelTransformerTest,
// TwitterStatusToAvroTransformerTest) - this one was the one sibling with no
// coverage at all.
class ElasticToResponseModelTransformerTest {

    private final ElasticToResponseModelTransformer transformer = new ElasticToResponseModelTransformer();

    private TwitterIndexModel indexModel(String id, long userId, String text, ZonedDateTime createdAt) {
        return TwitterIndexModel.builder().id(id).userId(userId).text(text).createdAt(createdAt).build();
    }

    @Test
    void getResponseModel_mapsEveryFieldAcrossOneToOne() {
        ZonedDateTime createdAt = ZonedDateTime.parse("2026-08-01T12:00:00Z");
        TwitterIndexModel source = indexModel("tweet-1", 42L, "hello world", createdAt);

        ElasticQueryServiceResponseModel result = transformer.getResponseModel(source);

        assertThat(result.getId()).isEqualTo("tweet-1");
        assertThat(result.getUserId()).isEqualTo(42L);
        assertThat(result.getText()).isEqualTo("hello world");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void getResponseModels_mapsEveryElementPreservingOrder() {
        List<TwitterIndexModel> source = List.of(
                indexModel("tweet-1", 1L, "first", ZonedDateTime.now()),
                indexModel("tweet-2", 2L, "second", ZonedDateTime.now())
        );

        List<ElasticQueryServiceResponseModel> result = transformer.getResponseModels(source);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("tweet-1");
        assertThat(result.get(1).getId()).isEqualTo("tweet-2");
    }

    @Test
    void getResponseModels_emptyList_returnsEmptyListNotNull() {
        assertThat(transformer.getResponseModels(List.of())).isEmpty();
    }
}
