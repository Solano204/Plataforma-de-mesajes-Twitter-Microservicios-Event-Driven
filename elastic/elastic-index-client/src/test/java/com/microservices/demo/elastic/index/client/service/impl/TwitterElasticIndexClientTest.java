package com.microservices.demo.elastic.index.client.service.impl;

import com.microservices.demo.config.ElasticConfigData;
import com.microservices.demo.elastic.index.client.util.ElasticIndexUtil;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexedObjectInformation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticIndexClientTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private final ElasticIndexUtil<TwitterIndexModel> elasticIndexUtil = new ElasticIndexUtil<>();

    private ElasticConfigData configData() {
        ElasticConfigData data = new ElasticConfigData();
        data.setIndexName("twitter-index");
        return data;
    }

    private TwitterElasticIndexClient client() {
        return new TwitterElasticIndexClient(configData(), elasticsearchOperations, elasticIndexUtil);
    }

    private IndexedObjectInformation indexed(String id) {
        return new IndexedObjectInformation(id, null, null, null, null);
    }

    @Test
    void save_delegatesToBulkIndexAgainstTheConfiguredIndex_andReturnsTheGeneratedIds() {
        TwitterIndexModel document = TwitterIndexModel.builder().id("tweet-1").userId(1L).text("hi")
                .createdAt(ZonedDateTime.now()).build();
        when(elasticsearchOperations.bulkIndex(any(), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(List.of(indexed("tweet-1")));

        List<String> ids = client().save(List.of(document));

        assertThat(ids).containsExactly("tweet-1");
    }

    @Test
    void save_passesOneIndexQueryPerDocumentToBulkIndex() {
        TwitterIndexModel doc1 = TwitterIndexModel.builder().id("tweet-1").userId(1L).text("a").createdAt(ZonedDateTime.now()).build();
        TwitterIndexModel doc2 = TwitterIndexModel.builder().id("tweet-2").userId(2L).text("b").createdAt(ZonedDateTime.now()).build();
        when(elasticsearchOperations.bulkIndex(any(), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(List.of(indexed("tweet-1"), indexed("tweet-2")));

        client().save(List.of(doc1, doc2));

        org.mockito.ArgumentCaptor<List<IndexQuery>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(elasticsearchOperations).bulkIndex(captor.capture(), eq(IndexCoordinates.of("twitter-index")));
        assertThat(captor.getValue()).extracting(IndexQuery::getId).containsExactly("tweet-1", "tweet-2");
    }

    @Test
    void save_emptyDocumentList_returnsEmptyIdList() {
        when(elasticsearchOperations.bulkIndex(any(), eq(IndexCoordinates.of("twitter-index")))).thenReturn(List.of());

        assertThat(client().save(List.of())).isEmpty();
    }
}
