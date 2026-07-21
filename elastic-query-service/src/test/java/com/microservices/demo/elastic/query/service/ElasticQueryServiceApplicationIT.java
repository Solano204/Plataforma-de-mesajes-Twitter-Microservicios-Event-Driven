package com.microservices.demo.elastic.query.service;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context integration test. Requires the live stack (config-server, Eureka,
 * Postgres, Keycloak) to be running - see docker-compose. Runs under the
 * "integration-test"/"verify" Maven phase via failsafe, not during a plain build.
 */
@SpringBootTest
public class ElasticQueryServiceApplicationIT {
    @Test
    public void contextLoads() {
    }
}
