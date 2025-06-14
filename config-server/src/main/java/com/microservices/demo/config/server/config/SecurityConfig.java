package com.microservices.demo.config.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain webSecurityCustomizer(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                new AntPathRequestMatcher("/actuator/**"),
                                new AntPathRequestMatcher("/encrypt/**"),
                                new AntPathRequestMatcher("/decrypt/**"),
                                new AntPathRequestMatcher("/**/health"),
                                new AntPathRequestMatcher("/*/default/**"),  // Allow config access
                                new AntPathRequestMatcher("/*/master/**"),   // Allow config access
                                new AntPathRequestMatcher("/*/main/**")      // Allow config access
                        )
                        .permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());  // Disable CSRF for REST endpoints
        return http.build();
    }
}