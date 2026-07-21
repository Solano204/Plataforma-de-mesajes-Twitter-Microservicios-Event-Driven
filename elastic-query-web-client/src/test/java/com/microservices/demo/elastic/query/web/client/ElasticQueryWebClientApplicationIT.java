package com.microservices.demo.elastic.query.web.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context integration test. Requires the live stack (config-server, Eureka,
 * Keycloak) to be running - see docker-compose. Runs under the "integration-test"/
 * "verify" Maven phase via failsafe, not during a plain build.
 */
@SpringBootTest
public class ElasticQueryWebClientApplicationIT {

    @Test
    public void contextLoads() {

    }
}
