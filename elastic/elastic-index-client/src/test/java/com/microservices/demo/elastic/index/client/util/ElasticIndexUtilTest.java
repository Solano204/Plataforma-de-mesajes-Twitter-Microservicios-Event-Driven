package com.microservices.demo.elastic.index.client.util;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticIndexUtilTest {

    private final ElasticIndexUtil<TwitterIndexModel> util = new ElasticIndexUtil<>();

    private TwitterIndexModel model(String id) {
        return TwitterIndexModel.builder().id(id).userId(1L).text("hi").createdAt(ZonedDateTime.now()).build();
    }

    @Test
    void getIndexQueries_oneDocument_buildsOneQueryWithMatchingIdAndObject() {
        TwitterIndexModel document = model("tweet-1");

        List<IndexQuery> queries = util.getIndexQueries(List.of(document));

        assertThat(queries).hasSize(1);
        assertThat(queries.get(0).getId()).isEqualTo("tweet-1");
        assertThat(queries.get(0).getObject()).isSameAs(document);
    }

    @Test
    void getIndexQueries_multipleDocuments_buildsOneQueryPerDocumentPreservingOrder() {
        List<TwitterIndexModel> documents = List.of(model("tweet-1"), model("tweet-2"), model("tweet-3"));

        List<IndexQuery> queries = util.getIndexQueries(documents);

        assertThat(queries).hasSize(3);
        assertThat(queries).extracting(IndexQuery::getId).containsExactly("tweet-1", "tweet-2", "tweet-3");
    }

    @Test
    void getIndexQueries_emptyList_returnsEmptyListNotNull() {
        assertThat(util.getIndexQueries(List.of())).isEmpty();
    }
}
