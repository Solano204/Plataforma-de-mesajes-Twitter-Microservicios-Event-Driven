package com.microservices.demo.elastic.query.service.model.assembler;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.elastic.query.service.common.model.ElasticQueryServiceResponseModel;
import com.microservices.demo.elastic.query.service.common.transformer.ElasticToResponseModelTransformer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// linkTo(methodOn(...)) needs a real HTTP request bound to the current
// thread to build absolute URIs from (that's how it knows the host/port/
// context-path) - outside a real MockMvc-dispatched request, that means
// manually binding one via RequestContextHolder, same as Spring's own HATEOAS
// test utilities do under the hood.
class ElasticQueryServiceResponseModelAssemblerTest {

    private final ElasticQueryServiceResponseModelAssembler assembler =
            new ElasticQueryServiceResponseModelAssembler(new ElasticToResponseModelTransformer());

    @BeforeEach
    void bindMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private TwitterIndexModel indexModel(String id) {
        return TwitterIndexModel.builder().id(id).userId(1L).text("hello").createdAt(ZonedDateTime.now()).build();
    }

    @Test
    void toModel_mapsTheUnderlyingFieldsAndAddsSelfAndDocumentsLinks() {
        ElasticQueryServiceResponseModel result = assembler.toModel(indexModel("tweet-1"));

        assertThat(result.getId()).isEqualTo("tweet-1");
        assertThat(result.getLinks()).hasSize(2);
        assertThat(result.getLink("self")).isPresent();
        assertThat(result.getLink("self").get().getHref()).contains("/documents/tweet-1");
        assertThat(result.getLink("documents")).isPresent();
        assertThat(result.getLink("documents").get().getHref()).endsWith("/documents");
    }

    @Test
    void toModels_mapsEveryElementInOrderEachWithItsOwnLinks() {
        List<ElasticQueryServiceResponseModel> results =
                assembler.toModels(List.of(indexModel("tweet-1"), indexModel("tweet-2")));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getId()).isEqualTo("tweet-1");
        assertThat(results.get(1).getId()).isEqualTo("tweet-2");
        assertThat(results.get(0).getLink("self").get().getHref()).contains("tweet-1");
        assertThat(results.get(1).getLink("self").get().getHref()).contains("tweet-2");
    }

    @Test
    void toModels_emptyList_returnsEmptyListNotNull() {
        assertThat(assembler.toModels(List.of())).isEmpty();
    }
}
