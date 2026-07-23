package com.microservices.demo.elastic.query.service.security;

import com.microservices.demo.config.ElasticQueryServiceConfigData;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private static final String REQUIRED_AUDIENCE = "elastic-query-service";

    private AudienceValidator validator() {
        ElasticQueryServiceConfigData configData = new ElasticQueryServiceConfigData();
        configData.setCustomAudience(REQUIRED_AUDIENCE);
        return new AudienceValidator(configData);
    }

    private Jwt jwtWithAudience(List<String> audience) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .claim("aud", audience)
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    @Test
    void validate_jwtContainsRequiredAudience_succeeds() {
        Jwt jwt = jwtWithAudience(List.of(REQUIRED_AUDIENCE));

        OAuth2TokenValidatorResult result = validator().validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_jwtHasOtherAudiencesToo_stillSucceedsAsLongAsTheRequiredOneIsPresent() {
        Jwt jwt = jwtWithAudience(List.of("some-other-service", REQUIRED_AUDIENCE));

        OAuth2TokenValidatorResult result = validator().validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void validate_jwtMissingRequiredAudience_fails() {
        Jwt jwt = jwtWithAudience(List.of("some-other-service"));

        OAuth2TokenValidatorResult result = validator().validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().iterator().next().getDescription())
                .contains(REQUIRED_AUDIENCE)
                .contains("is missing");
    }

    @Test
    void validate_jwtWithEmptyAudienceList_fails() {
        Jwt jwt = jwtWithAudience(List.of());

        OAuth2TokenValidatorResult result = validator().validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    void validate_errorCodeIsInvalidToken() {
        Jwt jwt = jwtWithAudience(List.of("wrong-audience"));

        OAuth2TokenValidatorResult result = validator().validate(jwt);

        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }
}
