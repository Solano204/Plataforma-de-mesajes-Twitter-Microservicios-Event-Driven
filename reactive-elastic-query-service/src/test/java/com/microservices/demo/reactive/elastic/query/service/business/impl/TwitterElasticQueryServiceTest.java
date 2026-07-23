package com.microservices.demo.reactive.elastic.query.service.business.impl;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.common.transformer.ElasticToResponseModelTransformer;
import com.microservices.demo.reactive.elastic.query.service.business.ReactiveElasticQueryClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterElasticQueryServiceTest {

    @Mock
    private ReactiveElasticQueryClient<TwitterIndexModel> reactiveElasticQueryClient;

    @Mock
    private ElasticToResponseModelTransformer elasticToResponseModelTransformer;

    @Test
    void getDocumentByText_mapsEachIndexModelThroughTheTransformer() {
        TwitterElasticQueryService service =
                new TwitterElasticQueryService(reactiveElasticQueryClient, elasticToResponseModelTransformer);

        TwitterIndexModel indexModel = TwitterIndexModel.builder()
                .id("1").userId(1L).text("hello").createdAt(ZonedDateTime.now()).build();
        ElasticQueryServiceResponseModel responseModel = ElasticQueryServiceResponseModel.builder()
                .id("1").userId(1L).text("hello").build();

        when(reactiveElasticQueryClient.getIndexModelByText("hello")).thenReturn(Flux.just(indexModel));
        when(elasticToResponseModelTransformer.getResponseModel(indexModel)).thenReturn(responseModel);

        StepVerifier.create(service.getDocumentByText("hello"))
                .expectNext(responseModel)
                .verifyComplete();
    }

    @Test
    void getDocumentByText_propagatesAnEmptyResult() {
        TwitterElasticQueryService service =
                new TwitterElasticQueryService(reactiveElasticQueryClient, elasticToResponseModelTransformer);

        when(reactiveElasticQueryClient.getIndexModelByText("missing")).thenReturn(Flux.empty());

        StepVerifier.create(service.getDocumentByText("missing"))
                .verifyComplete();
    }
}
