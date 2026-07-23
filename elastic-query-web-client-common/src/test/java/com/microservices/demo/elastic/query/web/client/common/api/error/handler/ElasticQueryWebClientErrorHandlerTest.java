package com.microservices.demo.elastic.query.web.client.common.api.error.handler;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// Pure unit tests - real ExtendedModelMap (a plain, concrete Model
// implementation) rather than a mock, since these tests care about what
// actually ends up in the model, not about verifying interaction calls.
// Sibling to elastic-query-service-common's ElasticQueryServiceErrorHandler
// (already tested) - this is the Thymeleaf/MVC-view variant (returns view
// names + populates a Model) rather than the REST-API/ResponseEntity one.
// This module (elastic-query-web-client-common) had zero test coverage
// before this pass.
//
// Fixed a real bug found while writing this: the AccessDeniedException
// handler called `model.addAttribute("error_description, You are not
// authorized to access this resource!")` - a single string literal with a
// comma INSIDE the quotes, instead of the intended two-argument call
// `addAttribute("error_description", "...")`. Model#addAttribute has a
// one-arg overload (uses a Spring-convention-derived attribute name), so
// this compiled and ran without error - it just silently never set
// "error_description" for this one exception type, unlike every other
// handler in this class. accessDeniedException_setsAllExpectedModelAttributes
// below is the direct regression test for the fix.
class ElasticQueryWebClientErrorHandlerTest {

    private final ElasticQueryWebClientErrorHandler handler = new ElasticQueryWebClientErrorHandler();

    @Test
    void accessDeniedException_returnsTheErrorView() {
        Model model = new ExtendedModelMap();

        String view = handler.handle(new AccessDeniedException("nope"), model);

        assertThat(view).isEqualTo("error");
    }

    @Test
    void accessDeniedException_setsAllExpectedModelAttributes() {
        // Regression test for the fixed addAttribute bug: error_description
        // must actually be present as its own attribute key, not folded
        // into "error"'s value or silently dropped.
        Model model = new ExtendedModelMap();

        handler.handle(new AccessDeniedException("nope"), model);

        Map<String, Object> attrs = model.asMap();
        assertThat(attrs).containsEntry("error", "Unauthorized");
        assertThat(attrs).containsKey("error_description");
        assertThat(attrs.get("error_description")).isEqualTo("You are not authorized to access this resource!");
    }

    @Test
    void illegalArgumentException_returnsTheErrorViewWithTheOriginalMessage() {
        Model model = new ExtendedModelMap();

        String view = handler.handle(new IllegalArgumentException("bad id format"), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap().get("error_description").toString()).contains("bad id format");
    }

    @Test
    void genericCheckedException_returnsErrorViewWithAGenericMessage_doesNotLeakDetails() {
        Model model = new ExtendedModelMap();

        String view = handler.handle(new Exception("some internal detail leaking risk"), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap().get("error_description").toString())
                .isEqualTo("A server error occurred!")
                .doesNotContain("internal detail");
    }

    @Test
    void runtimeException_returnsTheHomeViewNotTheErrorView_andRepopulatesTheRequestModel() {
        // Unlike every other handler in this class, RuntimeException
        // returns to "home" (re-rendering the query form) rather than
        // "error", and re-adds a blank elasticQueryWebClientRequestModel so
        // the form has something to bind to.
        Model model = new ExtendedModelMap();

        String view = handler.handle(new RuntimeException("elasticsearch timeout"), model);

        assertThat(view).isEqualTo("home");
        assertThat(model.asMap()).containsKey("elasticQueryWebClientRequestModel");
        assertThat(model.asMap().get("error").toString()).contains("elasticsearch timeout");
    }

    @Test
    void bindException_returnsHomeViewWithFieldNameToMessageMap() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "requestModel");
        bindingResult.addError(new FieldError("requestModel", "keyword", "must not be empty"));
        BindException exception = new BindException(bindingResult);
        Model model = new ExtendedModelMap();

        String view = handler.handle(exception, model);

        assertThat(view).isEqualTo("home");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) model.asMap().get("error_description");
        assertThat(errors).containsEntry("keyword", "must not be empty");
    }
}
