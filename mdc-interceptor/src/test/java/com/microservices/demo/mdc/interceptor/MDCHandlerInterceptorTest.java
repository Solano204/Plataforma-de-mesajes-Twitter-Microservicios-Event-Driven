package com.microservices.demo.mdc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.util.IdGenerator;

import java.util.UUID;

import static com.microservices.demo.mdc.Constants.CORRELATION_ID_HEADER;
import static com.microservices.demo.mdc.Constants.CORRELATION_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// This interceptor is the whole system's distributed-tracing correlation-ID
// propagation - every request across every service either carries forward an
// upstream ID or mints a fresh one, and MDC state has to be cleaned up after
// every request or it leaks into whatever the next request on that thread
// logs. Had zero tests despite running on every single HTTP request.
@ExtendWith(MockitoExtension.class)
class MDCHandlerInterceptorTest {

    @Mock
    private IdGenerator idGenerator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @AfterEach
    void cleanupRealMdc() {
        // preHandle/afterCompletion touch the real static MDC - don't leak
        // state into other tests in the same JVM/thread.
        MDC.clear();
    }

    @Test
    void preHandle_incomingCorrelationIdHeaderPresent_reusesItInsteadOfGeneratingANewOne() throws Exception {
        // Arrange
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("upstream-correlation-id-123");
        MDCHandlerInterceptor interceptor = new MDCHandlerInterceptor(idGenerator);

        // Act
        boolean result = interceptor.preHandle(request, response, new Object());

        // Assert
        assertThat(result).isTrue();
        assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo("upstream-correlation-id-123");
        verify(idGenerator, org.mockito.Mockito.never()).generateId();
    }

    @Test
    void preHandle_noIncomingHeader_generatesAFreshCorrelationId() throws Exception {
        UUID generated = UUID.randomUUID();
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn(null);
        when(idGenerator.generateId()).thenReturn(generated);
        MDCHandlerInterceptor interceptor = new MDCHandlerInterceptor(idGenerator);

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(generated.toString());
    }

    @Test
    void preHandle_emptyStringHeader_treatedAsMissingGeneratesAFreshId() throws Exception {
        UUID generated = UUID.randomUUID();
        when(request.getHeader(CORRELATION_ID_HEADER)).thenReturn("");
        when(idGenerator.generateId()).thenReturn(generated);
        MDCHandlerInterceptor interceptor = new MDCHandlerInterceptor(idGenerator);

        interceptor.preHandle(request, response, new Object());

        assertThat(MDC.get(CORRELATION_ID_KEY)).isEqualTo(generated.toString());
    }

    @Test
    void afterCompletion_removesTheCorrelationIdFromMdc_evenAfterAnException() throws Exception {
        MDC.put(CORRELATION_ID_KEY, "some-id-still-set-from-preHandle");
        MDCHandlerInterceptor interceptor = new MDCHandlerInterceptor(idGenerator);

        interceptor.afterCompletion(request, response, new Object(), new RuntimeException("handler blew up"));

        assertThat(MDC.get(CORRELATION_ID_KEY)).isNull();
    }
}
