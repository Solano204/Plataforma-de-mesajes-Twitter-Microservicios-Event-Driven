package com.microservices.demo.elastic.query.web.client.api;

import com.microservices.demo.elastic.query.web.client.common.model.ElasticQueryWebClientRequestModel;
import com.microservices.demo.elastic.query.web.client.common.model.ElasticQueryWebClientResponseModel;
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
        List<ElasticQueryWebClientResponseModel> responseModels =
                List.of(ElasticQueryWebClientResponseModel.builder().text("hello").build());
        when(elasticQueryWebClient.getDataByText(request)).thenReturn(responseModels);
        Model model = new ExtendedModelMap();

        String view = controller.queryByText(request, model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("elasticQueryWebClientResponseModels")).isEqualTo(responseModels);
        assertThat(model.getAttribute("searchText")).isEqualTo("hello");
    }

    @Test
    void home_returnsHomeViewWithAnEmptyRequestModel() {
        Model model = new ExtendedModelMap();

        String view = controller.home(model);

        assertThat(view).isEqualTo("home");
        assertThat(model.getAttribute("elasticQueryWebClientRequestModel")).isNotNull();
    }
}
