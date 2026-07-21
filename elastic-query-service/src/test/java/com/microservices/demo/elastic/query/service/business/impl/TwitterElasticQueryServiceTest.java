package com.microservices.demo.elastic.query.service.business.impl;

import com.microservices.demo.config.ElasticQueryServiceConfigData;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.client.service.ElasticQueryClient;
import com.microservices.demo.elastic.query.service.QueryType;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.model.ElasticQueryServiceAnalyticsResponseModel;
import com.microservices.demo.elastic.query.service.model.assembler.ElasticQueryServiceResponseModelAssembler;
import com.microservices.demo.elastic.query.service.port.out.WordCountQueryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticQueryServiceTest {

    @Mock
    private ElasticQueryServiceResponseModelAssembler assembler;

    @Mock
    private ElasticQueryClient<TwitterIndexModel> elasticQueryClient;

    @Mock
    private WordCountQueryPort wordCountQueryPort;

    private ElasticQueryServiceConfigData configData;

    private TwitterElasticQueryService service;

    @BeforeEach
    void setUp() {
        configData = new ElasticQueryServiceConfigData();
        service = new TwitterElasticQueryService(assembler, elasticQueryClient, configData, wordCountQueryPort);
    }

    @Test
    void getDocumentById_returnsAssembledModel() {
        TwitterIndexModel indexModel = TwitterIndexModel.builder()
                .id("1")
                .userId(42L)
                .text("hello world")
                .createdAt(ZonedDateTime.now())
                .build();
        ElasticQueryServiceResponseModel responseModel = new ElasticQueryServiceResponseModel();
        when(elasticQueryClient.getIndexModelById("1")).thenReturn(indexModel);
        when(assembler.toModel(indexModel)).thenReturn(responseModel);

        ElasticQueryServiceResponseModel result = service.getDocumentById("1");

        assertThat(result).isSameAs(responseModel);
    }

    @Test
    void getAllDocuments_delegatesToClientAndAssembler() {
        List<TwitterIndexModel> indexModels = List.of(TwitterIndexModel.builder().id("1").build());
        List<ElasticQueryServiceResponseModel> responseModels = List.of(new ElasticQueryServiceResponseModel());
        when(elasticQueryClient.getAllIndexModels()).thenReturn(indexModels);
        when(assembler.toModels(indexModels)).thenReturn(responseModels);

        List<ElasticQueryServiceResponseModel> result = service.getAllDocuments();

        assertThat(result).isSameAs(responseModels);
    }

    @Test
    void getDocumentByText_queriesAnalyticsDatabase_whenConfigured() {
        ElasticQueryServiceConfigData.WebClient webClientConfig = new ElasticQueryServiceConfigData.WebClient();
        webClientConfig.setQueryType(QueryType.ANALYTICS_DATABASE.getType());
        configData.setWebClient(webClientConfig);

        when(elasticQueryClient.getIndexModelByText("hello")).thenReturn(List.of());
        when(assembler.toModels(any())).thenReturn(List.of());
        when(wordCountQueryPort.getWordCount(eq(QueryType.ANALYTICS_DATABASE), eq("hello"), eq("token")))
                .thenReturn(7L);

        ElasticQueryServiceAnalyticsResponseModel result = service.getDocumentByText("hello", "token");

        assertThat(result.getWordCount()).isEqualTo(7L);
        verify(wordCountQueryPort).getWordCount(QueryType.ANALYTICS_DATABASE, "hello", "token");
    }

    @Test
    void getDocumentByText_queriesKafkaStateStore_whenConfigured() {
        ElasticQueryServiceConfigData.WebClient webClientConfig = new ElasticQueryServiceConfigData.WebClient();
        webClientConfig.setQueryType(QueryType.KAFKA_STATE_STORE.getType());
        configData.setWebClient(webClientConfig);

        when(elasticQueryClient.getIndexModelByText("hello")).thenReturn(List.of());
        when(assembler.toModels(any())).thenReturn(List.of());
        when(wordCountQueryPort.getWordCount(QueryType.KAFKA_STATE_STORE, "hello", "token"))
                .thenReturn(3L);

        ElasticQueryServiceAnalyticsResponseModel result = service.getDocumentByText("hello", "token");

        assertThat(result.getWordCount()).isEqualTo(3L);
        verify(wordCountQueryPort).getWordCount(QueryType.KAFKA_STATE_STORE, "hello", "token");
    }
}
