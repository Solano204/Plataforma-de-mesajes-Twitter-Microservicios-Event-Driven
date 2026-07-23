package com.microservices.demo.elastic.query.web.client.api;

import com.microservices.demo.elastic.query.web.client.common.model.ElasticQueryWebClientAnalyticsResponseModel;
import com.microservices.demo.elastic.query.web.client.common.model.ElasticQueryWebClientRequestModel;
import com.microservices.demo.elastic.query.web.client.service.ElasticQueryWebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryControllerTest {

    @Mock
    private ElasticQueryWebClient elasticQueryWebClient;

    private QueryController controller;

    @BeforeEach
    void setUp() {
        controller = new QueryController(elasticQueryWebClient);
    }

    @Test
    void queryByText_populatesModelFromTheClientResponse() {
        ElasticQueryWebClientRequestModel request = ElasticQueryWebClientRequestModel.builder()
                .text("hello").build();
        ElasticQueryWebClientAnalyticsResponseModel response = ElasticQueryWebClientAnalyticsResponseModel.builder()
                .queryResponseModels(List.of())
                .wordCount(5L)
                .fallbackMessage("fallback")
                .build();
        when(elasticQueryWebClient.getDataByText(request)).thenReturn(response);
        Model model = new ExtendedModelMap();

        String view = controller.queryByText(request, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("wordCount")).isEqualTo(5L);
        assertThat(model.getAttribute("fallbackMessage")).isEqualTo("fallback");
        assertThat(model.getAttribute("searchText")).isEqualTo("hello");
        assertThat(model.getAttribute("elasticQueryWebClientResponseModels")).isEqualTo(List.of());
    }

    @Test
    void home_returnsHomeViewWithAnEmptyRequestModel() {
        Model model = new ExtendedModelMap();

        String view = controller.home(model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("elasticQueryWebClientRequestModel")).isNotNull();
    }
}
