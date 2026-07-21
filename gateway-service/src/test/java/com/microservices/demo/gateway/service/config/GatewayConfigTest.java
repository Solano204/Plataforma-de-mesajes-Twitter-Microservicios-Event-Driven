package com.microservices.demo.gateway.service.config;

import com.microservices.demo.config.GatewayServiceConfigData;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayConfigTest {

    private GatewayConfig config() {
        return new GatewayConfig(new GatewayServiceConfigData());
    }

    @Test
    void userKeyResolver_requestWithAuthorizationHeader_resolvesToItsValue() {
        KeyResolver resolver = config().userKeyResolver();
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/documents").header(HttpHeaders.AUTHORIZATION, "Bearer token-value"));

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("Bearer token-value")
                .verifyComplete();
    }

    @Test
    void userKeyResolver_requestWithoutAuthorizationHeader_throwsNullPointerExceptionSynchronously() {
        // documents current behavior: Objects.requireNonNull(...) runs
        // eagerly while building the Mono (before anyone subscribes), so a
        // missing Authorization header blows up synchronously right out of
        // resolve() rather than as a reactive onError signal.
        KeyResolver resolver = config().userKeyResolver();
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/documents"));

        assertThatThrownBy(() -> resolver.resolve(exchange)).isInstanceOf(NullPointerException.class);
    }
}
