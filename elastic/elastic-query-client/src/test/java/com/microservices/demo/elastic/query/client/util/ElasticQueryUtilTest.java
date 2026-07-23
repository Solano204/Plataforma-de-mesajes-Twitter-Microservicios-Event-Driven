package com.microservices.demo.elastic.query.client.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticQueryUtilTest {

    private final ElasticQueryUtil<TwitterIndexModel> util = new ElasticQueryUtil<>();

    @Test
    void getSearchQueryById_buildsAnIdsOnlyQueryWithTheGivenId() {
        NativeQuery query = (NativeQuery) util.getSearchQueryById("tweet-1");

        assertThat(query.getIds()).containsExactly("tweet-1");
        assertThat(query.getQuery()).isNull();
    }

    @Test
    void getSearchQueryByFieldText_buildsAMatchQueryOnTheGivenFieldAndText() {
        NativeQuery query = (NativeQuery) util.getSearchQueryByFieldText("text", "hello world");

        Query esQuery = query.getQuery();
        assertThat(esQuery).isNotNull();
        assertThat(esQuery.isMatch()).isTrue();
        assertThat(esQuery.match().field()).isEqualTo("text");
        assertThat(esQuery.match().query().stringValue()).isEqualTo("hello world");
    }

    @Test
    void getSearchQueryForAll_buildsAMatchAllQuery() {
        NativeQuery query = (NativeQuery) util.getSearchQueryForAll();

        assertThat(query.getQuery()).isNotNull();
        assertThat(query.getQuery().isMatchAll()).isTrue();
    }
}
