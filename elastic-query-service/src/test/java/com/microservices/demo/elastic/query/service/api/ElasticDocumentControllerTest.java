package com.microservices.demo.elastic.query.service.api;

import com.microservices.demo.elastic.query.service.business.ElasticQueryService;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceRequestModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.model.ElasticQueryServiceAnalyticsResponseModel;
import com.microservices.demo.elastic.query.service.model.ElasticQueryServiceResponseModelV2;
import com.microservices.demo.elastic.query.service.security.TwitterQueryUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticDocumentControllerTest {

    @Mock
    private ElasticQueryService elasticQueryService;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    private ElasticDocumentController controller() {
        ElasticDocumentController controller = new ElasticDocumentController(elasticQueryService);
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "port", "8080");
        return controller;
    }

    private ElasticQueryServiceResponseModel responseModel(String id) {
        return ElasticQueryServiceResponseModel.builder().id(id).userId(1L).text("hi").createdAt(ZonedDateTime.now()).build();
    }

    private TwitterQueryUser principal() {
        return TwitterQueryUser.builder().username("alice").permissions(Map.of()).build();
    }

    @Test
    void getAllDocuments_returnsWhatTheServiceReturns() {
        List<ElasticQueryServiceResponseModel> docs = List.of(responseModel("tweet-1"), responseModel("tweet-2"));
        when(elasticQueryService.getAllDocuments()).thenReturn(docs);

        ResponseEntity<List<ElasticQueryServiceResponseModel>> response = controller().getAllDocuments();

        assertThat(response.getBody()).isEqualTo(docs);
    }

    @Test
    void getDocumentById_returnsWhatTheServiceReturns() {
        ElasticQueryServiceResponseModel doc = responseModel("tweet-1");
        when(elasticQueryService.getDocumentById("tweet-1")).thenReturn(doc);

        ResponseEntity<ElasticQueryServiceResponseModel> response = controller().getDocumentById("tweet-1");

        assertThat(response.getBody()).isSameAs(doc);
    }

    @Test
    void getDocumentByIdV2_mapsToV2ModelWithParsedNumericIdAndFixedText2() {
        ElasticQueryServiceResponseModel doc = responseModel("12345");
        when(elasticQueryService.getDocumentById("12345")).thenReturn(doc);

        ResponseEntity<ElasticQueryServiceResponseModelV2> response = controller().getDocumentByIdV2("12345");

        ElasticQueryServiceResponseModelV2 v2 = response.getBody();
        assertThat(v2.getId()).isEqualTo(12345L);
        assertThat(v2.getUserId()).isEqualTo(doc.getUserId());
        assertThat(v2.getText()).isEqualTo(doc.getText());
        assertThat(v2.getText2()).isEqualTo("Version 2 text");
    }

    @Test
    void getDocumentByIdV2_nonNumericId_throwsNumberFormatException() {
        // documents a real edge case: the v1->v2 mapper assumes the
        // document id is always numeric-parseable, which isn't guaranteed
        // by ElasticQueryServiceResponseModel's id type (a plain String).
        when(elasticQueryService.getDocumentById("not-a-number")).thenReturn(responseModel("not-a-number"));

        assertThatThrownBy(() -> controller().getDocumentByIdV2("not-a-number"))
                .isInstanceOf(NumberFormatException.class);
    }

    @Test
    void getDocumentByText_delegatesWithTheRequestTextAndTheAuthorizedClientsAccessToken() {
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-value",
                Instant.now(), Instant.now().plusSeconds(60));
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        ElasticQueryServiceAnalyticsResponseModel serviceResponse = ElasticQueryServiceAnalyticsResponseModel.builder()
                .wordCount(1L).queryResponseModels(List.of(responseModel("tweet-1"))).build();
        when(elasticQueryService.getDocumentByText("hello", "token-value")).thenReturn(serviceResponse);

        ElasticQueryServiceRequestModel request = ElasticQueryServiceRequestModel.builder().text("hello").build();
        ResponseEntity<ElasticQueryServiceAnalyticsResponseModel> response =
                controller().getDocumentByText(request, principal(), authorizedClient);

        assertThat(response.getBody()).isSameAs(serviceResponse);
    }
}
