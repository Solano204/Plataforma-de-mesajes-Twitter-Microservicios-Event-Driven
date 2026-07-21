package com.microservices.demo.kafka.admin.client;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.RetryConfigData;
import com.microservices.demo.kafka.admin.exception.KafkaClientException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// The startup-time "wait for Kafka topics/schema registry to actually be
// ready" resilience logic - had zero tests despite being the exact kind of
// retry/backoff code that's easy to get subtly wrong (off-by-one on max
// attempts, wrong exception wrapping, infinite loop on a bad exit
// condition). WebClient/checkSchemaRegistry() is intentionally NOT covered
// here - its fluent builder chain (.method().uri().exchangeToMono().block())
// would need several layers of mocking to get right, and with no way to run
// this and see it actually pass, that risk isn't worth taking blind; a real
// RetryTemplate is used instead of mocking it, since it needs to actually
// invoke the callback it's given.
@ExtendWith(MockitoExtension.class)
class KafkaAdminClientTest {

    @Mock
    private AdminClient adminClient;
    @Mock
    private WebClient webClient;

    private RetryConfigData fastRetryConfig() {
        RetryConfigData config = new RetryConfigData();
        config.setMaxAttempts(2);
        config.setInitialIntervalMs(1L);
        config.setMaxIntervalMs(2L);
        config.setMultiplier(1.0);
        config.setSleepTimeMs(1L); // real Thread.sleep() calls happen in checkTopicsCreated - keep them tiny
        return config;
    }

    private KafkaConfigData kafkaConfig(String... topics) {
        KafkaConfigData config = new KafkaConfigData();
        config.setTopicNamesToCreate(List.of(topics));
        config.setNumOfPartitions(1);
        config.setReplicationFactor((short) 1);
        config.setSchemaRegistryUrl("http://localhost:8081");
        return config;
    }

    @SuppressWarnings("unchecked")
    private void stubListTopics(TopicListing... listings) throws Exception {
        ListTopicsResult listTopicsResult = mock(ListTopicsResult.class);
        KafkaFuture<java.util.Collection<TopicListing>> future = mock(KafkaFuture.class);
        when(future.get()).thenReturn(List.of(listings));
        when(listTopicsResult.listings()).thenReturn(future);
        when(adminClient.listTopics()).thenReturn(listTopicsResult);
    }

    @Test
    void checkTopicsCreated_topicAlreadyExists_returnsImmediatelyNoException() throws Exception {
        TopicListing existing = mock(TopicListing.class);
        when(existing.name()).thenReturn("twitter-topic");
        stubListTopics(existing);

        KafkaAdminClient client = new KafkaAdminClient(
                kafkaConfig("twitter-topic"), fastRetryConfig(), adminClient, RetryTemplate.builder().maxAttempts(1).build(), webClient);

        assertThatCode(client::checkTopicsCreated).doesNotThrowAnyException();
    }

    @Test
    void checkTopicsCreated_topicNeverAppears_throwsKafkaClientExceptionAfterMaxRetries() throws Exception {
        stubListTopics(); // list always comes back empty - topic never "appears"

        KafkaAdminClient client = new KafkaAdminClient(
                kafkaConfig("twitter-topic"), fastRetryConfig(), adminClient, RetryTemplate.builder().maxAttempts(1).build(), webClient);

        assertThatThrownBy(client::checkTopicsCreated)
                .isInstanceOf(KafkaClientException.class)
                .hasMessageContaining("Reached max number of retry");
    }

    @Test
    void createTopics_adminClientSucceeds_callsCreateTopicsWithTheConfiguredTopicsThenVerifies() throws Exception {
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        when(createResult.values()).thenReturn(Collections.emptyMap());
        when(adminClient.createTopics(any())).thenReturn(createResult);

        TopicListing existing = mock(TopicListing.class);
        when(existing.name()).thenReturn("twitter-topic");
        stubListTopics(existing);

        KafkaAdminClient client = new KafkaAdminClient(
                kafkaConfig("twitter-topic"), fastRetryConfig(), adminClient,
                RetryTemplate.builder().maxAttempts(1).build(), webClient);

        client.createTopics();

        verify(adminClient).createTopics(any());
    }

    @Test
    void createTopics_adminClientAlwaysThrows_wrapsInKafkaClientExceptionAfterExhaustingRetries() {
        when(adminClient.createTopics(any())).thenThrow(new RuntimeException("broker unavailable"));

        KafkaAdminClient client = new KafkaAdminClient(
                kafkaConfig("twitter-topic"), fastRetryConfig(), adminClient,
                RetryTemplate.builder().maxAttempts(2).fixedBackoff(1).build(), webClient);

        assertThatThrownBy(client::createTopics)
                .isInstanceOf(KafkaClientException.class)
                .hasMessageContaining("Reached max number of retry for creating kafka topic");
    }
}
