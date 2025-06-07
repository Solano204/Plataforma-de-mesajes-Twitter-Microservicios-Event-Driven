package com.microservices.demo.gateway.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class WebSecurityConfig {

    @Bean
    public SecurityWebFilterChain webFluxSecurityConfig(ServerHttpSecurity httpSecurity) {
        httpSecurity.authorizeExchange()
                .anyExchange()
                .permitAll();
        httpSecurity.csrf().disable();
        return httpSecurity.build();
    }



    //  @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //             .authorizeHttpRequests(auth -> auth
    //                     .anyRequest().permitAll() // Equivalent to your WebFlux .anyExchange().permitAll()
    //             )
    //             .csrf(csrf -> csrf.disable()) // Equivalent to your WebFlux .csrf().disable()
    //             .sessionManagement(session -> session
    //                     .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Added for stateless operation
    //             );

    //     return http.build();
    // }
}
