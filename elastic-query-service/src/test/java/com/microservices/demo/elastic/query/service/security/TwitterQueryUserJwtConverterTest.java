package com.microservices.demo.elastic.query.service.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterQueryUserJwtConverterTest {

    @Mock
    private TwitterQueryUserDetailsService userDetailsService;

    private TwitterQueryUserJwtConverter converter() {
        return new TwitterQueryUserJwtConverter(userDetailsService);
    }

    private Jwt.Builder baseJwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("preferred_username", "alice");
    }

    private TwitterQueryUser aliceUser() {
        return TwitterQueryUser.builder().username("alice").permissions(Map.of()).build();
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
    void convert_noScopeClaim_returnsOnlyRoleAuthorities() {
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", List.of("admin")))
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_rolesClaimNotACollection_treatedAsNoRoles() {
        Jwt jwt = baseJwt()
                .claim("realm_access", Map.of("roles", "not-a-list"))
                .claim("scope", "read")
                .build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_READ");
    }

    @Test
    void convert_realmAccessClaimMissingEntirely_throwsNullPointerException() {
        // documents current behavior: getRoles() unconditionally casts+reads
        // realm_access without a null check, so a token that never went
        // through a realm-access-populating IdP blows up here rather than
        // degrading to "no roles"
        Jwt jwt = baseJwt().claim("scope", "read").build();

        assertThatThrownBy(() -> converter().convert(jwt)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void convert_setsTheResolvedAuthoritiesOntoThePrincipalItself() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of("admin"))).build();
        TwitterQueryUser user = aliceUser();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(user);

        converter().convert(jwt);

        assertThat(user.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void convert_usesThePreferredUsernameClaimToLookUpTheUser() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of())).build();
        when(userDetailsService.loadUserByUsername(eq("alice"))).thenReturn(aliceUser());

        AbstractAuthenticationToken token = converter().convert(jwt);

        assertThat(((TwitterQueryUser) token.getPrincipal()).getUsername()).isEqualTo("alice");
    }

    @Test
    void convert_userDetailsServiceThrowsUsernameNotFound_propagatesTheException() {
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of())).build();
        when(userDetailsService.loadUserByUsername("alice"))
                .thenThrow(new UsernameNotFoundException("No user found with name alice"));

        assertThatThrownBy(() -> converter().convert(jwt)).isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void convert_userDetailsServiceReturnsNull_throwsBadCredentialsException() {
        // UserDetailsService's contract technically allows a null return even
        // though this codebase's own impl never does it (it throws instead) -
        // the converter has its own orElseThrow() specifically for this case.
        Jwt jwt = baseJwt().claim("realm_access", Map.of("roles", List.of())).build();
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(null);

        assertThatThrownBy(() -> converter().convert(jwt)).isInstanceOf(BadCredentialsException.class);
    }
}
