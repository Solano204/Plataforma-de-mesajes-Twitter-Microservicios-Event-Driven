package com.microservices.demo.elastic.query.service.common.api.error.handler;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticQueryServiceErrorHandlerTest {

    private final ElasticQueryServiceErrorHandler handler = new ElasticQueryServiceErrorHandler();

    // MethodArgumentNotValidException.getMessage() (invoked when the handler
    // logs the exception) needs a MethodParameter backed by a real
    // Executable, so a Mockito mock isn't enough here - it NPEs deep inside
    // logback. Reflecting on this dummy method gives a real one cheaply.
    @SuppressWarnings("unused")
    private void dummyTarget(String text) { }

    private MethodParameter realMethodParameter() throws NoSuchMethodException {
        return new MethodParameter(
                ElasticQueryServiceErrorHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
    }

    @Test
    void handle_accessDeniedException_returns403WithAFixedMessage() {
        ResponseEntity<String> response = handler.handle(new AccessDeniedException("nope"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("You are not authorized to access this resource!");
    }

    @Test
    void handle_illegalArgumentException_returns400IncludingTheOriginalMessage() {
        ResponseEntity<String> response = handler.handle(new IllegalArgumentException("bad id format"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("bad id format");
    }

    @Test
    void handle_genericRuntimeException_returns400IncludingTheOriginalMessage() {
        ResponseEntity<String> response = handler.handle(new RuntimeException("elasticsearch timeout"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("elasticsearch timeout");
    }

    @Test
    void handle_arbitraryCheckedException_returns500WithAGenericMessage_doesNotLeakDetails() {
        ResponseEntity<String> response = handler.handle(new Exception("some internal detail leaking risk"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("A server error occurred!");
        assertThat(response.getBody()).doesNotContain("internal detail");
    }

    @Test
    void handle_methodArgumentNotValid_returnsFieldNameToMessageMapForEveryFieldError() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "requestModel");
        bindingResult.addError(new FieldError("requestModel", "text", "must not be empty"));
        bindingResult.addError(new FieldError("requestModel", "id", "must not be blank"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(realMethodParameter(), bindingResult);

        ResponseEntity<java.util.Map<String, String>> response = handler.handle(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("text", "must not be empty")
                .containsEntry("id", "must not be blank");
    }
}
