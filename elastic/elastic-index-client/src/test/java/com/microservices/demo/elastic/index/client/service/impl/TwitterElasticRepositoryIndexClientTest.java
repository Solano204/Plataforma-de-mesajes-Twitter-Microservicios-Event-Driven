package com.microservices.demo.elastic.index.client.service.impl;

import com.microservices.demo.elastic.index.client.repository.TwitterElasticsearchIndexRepository;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticRepositoryIndexClientTest {

    @Mock
    private TwitterElasticsearchIndexRepository repository;

    private TwitterElasticRepositoryIndexClient client() {
        return new TwitterElasticRepositoryIndexClient(repository);
    }

    private TwitterIndexModel model(String id) {
        return TwitterIndexModel.builder().id(id).userId(1L).text("hi").createdAt(ZonedDateTime.now()).build();
    }

    @Test
    void save_delegatesToRepositorySaveAll_andReturnsTheSavedDocumentIds() {
        List<TwitterIndexModel> saved = List.of(model("tweet-1"), model("tweet-2"));
        when(repository.saveAll(saved)).thenReturn(saved);

        List<String> ids = client().save(saved);

        assertThat(ids).containsExactly("tweet-1", "tweet-2");
    }

    @Test
    void save_emptyList_returnsEmptyIdList() {
        when(repository.saveAll(List.of())).thenReturn(List.of());

        assertThat(client().save(List.of())).isEmpty();
    }
}
