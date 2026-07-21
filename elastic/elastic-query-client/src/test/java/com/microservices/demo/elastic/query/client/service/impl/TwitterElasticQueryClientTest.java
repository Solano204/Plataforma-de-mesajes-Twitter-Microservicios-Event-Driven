package com.microservices.demo.elastic.query.client.service.impl;

import com.microservices.demo.config.ElasticConfigData;
import com.microservices.demo.config.ElasticQueryConfigData;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.client.exception.ElasticQueryClientException;
import com.microservices.demo.elastic.query.client.util.ElasticQueryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticQueryClientTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    private final ElasticQueryUtil<TwitterIndexModel> elasticQueryUtil = new ElasticQueryUtil<>();

    private ElasticConfigData configData() {
        ElasticConfigData data = new ElasticConfigData();
        data.setIndexName("twitter-index");
        return data;
    }

    private ElasticQueryConfigData queryConfigData() {
        ElasticQueryConfigData data = new ElasticQueryConfigData();
        data.setTextField("text");
        return data;
    }

    private TwitterElasticQueryClient client() {
        return new TwitterElasticQueryClient(configData(), queryConfigData(), elasticsearchOperations, elasticQueryUtil);
    }

    private TwitterIndexModel model(String id) {
        return TwitterIndexModel.builder().id(id).userId(1L).text("hi").createdAt(ZonedDateTime.now()).build();
    }

    @SuppressWarnings("unchecked")
    private SearchHit<TwitterIndexModel> searchHit(TwitterIndexModel content) {
        SearchHit<TwitterIndexModel> hit = mock(SearchHit.class);
        // lenient: getIndexModelById() logs searchResult.getId(), but the
        // list-returning paths (getIndexModelByText/getAllIndexModels) never
        // touch it - only .getContent() - so this stub goes unused there,
        // which strict-stubs would otherwise flag as unnecessary.
        org.mockito.Mockito.lenient().when(hit.getId()).thenReturn(content.getId());
        when(hit.getContent()).thenReturn(content);
        return hit;
    }

    // Must be fully built and assigned to a local BEFORE being passed to
    // when(...).thenReturn(...) - calling this (or searchHit()) directly as
    // a thenReturn() argument interleaves its own mock/when() setup with the
    // outer when() call still in progress, corrupting Mockito's stubbing
    // state (surfaces as a confusing UnfinishedStubbingException elsewhere).
    @SuppressWarnings("unchecked")
    private SearchHits<TwitterIndexModel> searchHits(TwitterIndexModel... contents) {
        SearchHits<TwitterIndexModel> hits = mock(SearchHits.class);
        List<SearchHit<TwitterIndexModel>> hitList = Stream.of(contents).map(this::searchHit).toList();
        when(hits.getTotalHits()).thenReturn((long) contents.length);
        when(hits.get()).thenReturn(hitList.stream());
        return hits;
    }

    @Test
    void getIndexModelById_found_returnsItsContent() {
        TwitterIndexModel document = model("tweet-1");
        SearchHit<TwitterIndexModel> hit = searchHit(document); // built BEFORE when() - see note on searchHits() below
        when(elasticsearchOperations.searchOne(any(), eq(TwitterIndexModel.class), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(hit);

        TwitterIndexModel result = client().getIndexModelById("tweet-1");

        assertThat(result).isSameAs(document);
    }

    @Test
    void getIndexModelById_notFound_throwsElasticQueryClientException() {
        when(elasticsearchOperations.searchOne(any(), eq(TwitterIndexModel.class), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(null);

        assertThatThrownBy(() -> client().getIndexModelById("missing-id"))
                .isInstanceOf(ElasticQueryClientException.class)
                .hasMessageContaining("missing-id");
    }

    @Test
    void getIndexModelByText_returnsEveryMatchingDocumentsContent() {
        TwitterIndexModel doc1 = model("tweet-1");
        TwitterIndexModel doc2 = model("tweet-2");
        SearchHits<TwitterIndexModel> hits = searchHits(doc1, doc2);
        when(elasticsearchOperations.search(any(Query.class), eq(TwitterIndexModel.class), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(hits);

        List<TwitterIndexModel> result = client().getIndexModelByText("hello");

        assertThat(result).containsExactly(doc1, doc2);
    }

    @Test
    void getAllIndexModels_returnsEveryDocumentsContent() {
        TwitterIndexModel doc1 = model("tweet-1");
        SearchHits<TwitterIndexModel> hits = searchHits(doc1);
        when(elasticsearchOperations.search(any(Query.class), eq(TwitterIndexModel.class), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(hits);

        assertThat(client().getAllIndexModels()).containsExactly(doc1);
    }

    @Test
    void getIndexModelByText_noMatches_returnsEmptyList() {
        SearchHits<TwitterIndexModel> hits = searchHits();
        when(elasticsearchOperations.search(any(Query.class), eq(TwitterIndexModel.class), eq(IndexCoordinates.of("twitter-index"))))
                .thenReturn(hits);

        assertThat(client().getIndexModelByText("nothing matches")).isEmpty();
    }
}
