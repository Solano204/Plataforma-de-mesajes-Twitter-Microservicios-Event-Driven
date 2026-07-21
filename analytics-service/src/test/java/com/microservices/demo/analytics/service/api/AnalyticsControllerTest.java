package com.microservices.demo.analytics.service.api;

import com.microservices.demo.analytics.service.business.AnalyticsService;
import com.microservices.demo.analytics.service.model.AnalyticsResponseModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private AnalyticsController controller() {
        return new AnalyticsController(analyticsService);
    }

    @Test
    void getWordCountByWord_found_returns200WithTheModel() {
        AnalyticsResponseModel model = AnalyticsResponseModel.builder().id(UUID.randomUUID()).word("hello").wordCount(5).build();
        when(analyticsService.getWordAnalytics("hello")).thenReturn(Optional.of(model));

        ResponseEntity<AnalyticsResponseModel> response = controller().getWordCountByWord("hello");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(model);
    }

    @Test
    void getWordCountByWord_notFound_stillReturns200ButWithAnEmptyBuilderDefaultModel() {
        // documents the current (arguably surprising) behavior: a missing
        // word is NOT a 404 here - it's a 200 with every field at its
        // builder default (null id/word, wordCount 0).
        when(analyticsService.getWordAnalytics("missing")).thenReturn(Optional.empty());

        ResponseEntity<AnalyticsResponseModel> response = controller().getWordCountByWord("missing");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isNull();
        assertThat(response.getBody().getWord()).isNull();
        assertThat(response.getBody().getWordCount()).isZero();
    }
}
