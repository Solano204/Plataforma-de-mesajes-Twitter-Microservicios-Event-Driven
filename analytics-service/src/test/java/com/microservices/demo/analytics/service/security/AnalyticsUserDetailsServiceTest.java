package com.microservices.demo.analytics.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsUserDetailsServiceTest {

    @Test
    void loadUserByUsername_returnsAUserBuiltWithThatExactUsername() {
        UserDetails result = new AnalyticsUserDetailsService().loadUserByUsername("alice");

        assertThat(result).isInstanceOf(AnalyticsUser.class);
        assertThat(result.getUsername()).isEqualTo("alice");
    }
}
