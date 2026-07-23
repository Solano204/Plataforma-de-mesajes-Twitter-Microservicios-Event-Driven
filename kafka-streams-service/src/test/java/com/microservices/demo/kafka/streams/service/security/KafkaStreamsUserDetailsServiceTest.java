package com.microservices.demo.kafka.streams.service.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaStreamsUserDetailsServiceTest {

    @Test
    void loadUserByUsername_returnsAUserBuiltWithThatExactUsername() {
        UserDetails result = new KafkaStreamsUserDetailsService().loadUserByUsername("alice");

        assertThat(result).isInstanceOf(KafkaStreamsUser.class);
        assertThat(result.getUsername()).isEqualTo("alice");
    }
}
