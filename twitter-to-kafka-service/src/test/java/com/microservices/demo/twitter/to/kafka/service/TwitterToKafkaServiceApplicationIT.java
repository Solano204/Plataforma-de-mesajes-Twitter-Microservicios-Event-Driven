package com.microservices.demo.twitter.to.kafka.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full-context integration test. Requires the live stack (config-server, Kafka)
 * to be running - see docker-compose. Runs under the "integration-test"/"verify"
 * Maven phase via failsafe, not during a plain build.
 */
@SpringBootTest
public class TwitterToKafkaServiceApplicationIT {

    @Test
    public void contextLoad() {

    }
}
