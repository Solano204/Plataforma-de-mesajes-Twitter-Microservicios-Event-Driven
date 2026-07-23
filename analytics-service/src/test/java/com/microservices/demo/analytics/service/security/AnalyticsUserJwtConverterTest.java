package com.microservices.demo.analytics.service.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsUserJwtConverterTest {

    @Mock
    private AnalyticsUserDetailsService userDetailsService;

    private AnalyticsUserJwtConverter converter() {
        return new AnalyticsUserJwtConverter(userDetailsService);
    }

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("preferred_username", "alice");
    }

    private AnalyticsUser aliceUser() {
        return AnalyticsUser.builder().username("alice").build();
    }

    @Test
    void convert_rolesAndScopePresent_combinesBothIntoPrefixedAuthorities() {
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", List.of("admin", "editor")))
                .claim("scope", "read write")
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EDITOR", "SCOPE_READ", "SCOPE_WRITE");
    }

    @Test
    void convert_rolesClaimNotACollection_gracefullyDegradesToScopesOnly() {
        // regression test: getRoles() used to fall back to an immutable
        // Collections.emptyList(), and getCombinedAuthorities() unconditionally
        // .addAll()s the scopes onto it - so a malformed (non-list) roles
        // claim used to blow up with UnsupportedOperationException instead of
        // degrading gracefully. Fixed to return a mutable list.
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .claim("scope", "read")
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("SCOPE_READ");
    }

    @Test
    void convert_noScopeClaim_returnsOnlyRoleAuthorities() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of("admin"))).build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_realmAccessClaimMissingEntirely_throwsNullPointerException() {
        Jwt jwt = baseJwt().claim("scope", "read").build();

        assertThatThrownBy(() -> converter().convert(jwt)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void convert_setsTheResolvedAuthoritiesOntoThePrincipalItself() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of("admin"))).build();
        AnalyticsUser user = aliceUser();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        converter().convert(jwt);

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_userDetailsServiceReturnsNull_throwsBadCredentialsException() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of())).build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(null);

        assertThatThrownBy(() -> converter().convert(jwt)).isInstanceOf(BadCredentialsException.class);
    }
}
