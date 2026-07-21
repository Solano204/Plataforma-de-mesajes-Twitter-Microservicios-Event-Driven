package com.microservices.demo.gateway.service.controller;

import com.microservices.demo.gateway.service.model.AnalyticsDataFallbackModel;
import com.microservices.demo.gateway.service.model.QueryServiceFallbackModel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void queryServiceFallback_returnsOkWithAFallbackMessage() {
        ResponseEntity<QueryServiceFallbackModel> response = controller.queryServiceFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFallbackMessage()).isNotBlank();
    }

    @Test
    void analyticsServiceFallback_returnsOkWithZeroWordCount() {
        ResponseEntity<AnalyticsDataFallbackModel> response = controller.analyticsServiceFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getWordCount()).isEqualTo(0L);
    }

    @Test
    void streamsServiceFallback_returnsOkWithZeroWordCount() {
        ResponseEntity<AnalyticsDataFallbackModel> response = controller.streamsServiceFallback();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getWordCount()).isEqualTo(0L);
    }
}
