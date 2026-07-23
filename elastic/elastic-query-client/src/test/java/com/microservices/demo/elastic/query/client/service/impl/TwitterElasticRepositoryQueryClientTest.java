package com.microservices.demo.elastic.query.client.service.impl;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.client.exception.ElasticQueryClientException;
import com.microservices.demo.elastic.query.client.repository.TwitterElasticsearchQueryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticRepositoryQueryClientTest {

    @Mock
    private TwitterElasticsearchQueryRepository repository;

    private TwitterElasticRepositoryQueryClient client() {
        return new TwitterElasticRepositoryQueryClient(repository);
    }

    private TwitterIndexModel model(String id) {
        return TwitterIndexModel.builder().id(id).userId(1L).text("hi").createdAt(ZonedDateTime.now()).build();
    }

    @Test
    void getIndexModelById_found_returnsIt() {
        TwitterIndexModel document = model("tweet-1");
        when(repository.findById("tweet-1")).thenReturn(Optional.of(document));

        assertThat(client().getIndexModelById("tweet-1")).isSameAs(document);
    }

    @Test
    void getIndexModelById_notFound_throwsElasticQueryClientException() {
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> client().getIndexModelById("missing-id"))
                .isInstanceOf(ElasticQueryClientException.class)
                .hasMessageContaining("missing-id");
    }

    @Test
    void getIndexModelByText_delegatesDirectlyToTheRepositoryQueryMethod() {
        List<TwitterIndexModel> results = List.of(model("tweet-1"), model("tweet-2"));
        when(repository.findByText("hello")).thenReturn(results);

        assertThat(client().getIndexModelByText("hello")).isEqualTo(results);
    }

    @Test
    void getAllIndexModels_convertsTheRepositoryIterableToAList() {
        when(repository.findAll()).thenReturn(List.of(model("tweet-1"), model("tweet-2")));

        List<TwitterIndexModel> result = client().getAllIndexModels();

        assertThat(result).extracting(TwitterIndexModel::getId).containsExactly("tweet-1", "tweet-2");
    }

    @Test
    void getAllIndexModels_noDocuments_returnsEmptyListNotNull() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(client().getAllIndexModels()).isEmpty();
    }
}
