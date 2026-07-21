package com.microservices.demo.kafka.streams.service.runner.impl;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.KafkaStreamsConfigData;
import com.microservices.demo.kafka.avro.model.TwitterAnalyticsAvroModel;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real Kafka Streams topology (word-count) via TopologyTestDriver -
 * no broker, no schema registry, no live infra needed.
 */
class KafkaStreamsRunnerTopologyTest {

    private static final String SCHEMA_REGISTRY_URL = "mock://kafka-streams-runner-test";
    private static final String INPUT_TOPIC = "input-topic";
    private static final String OUTPUT_TOPIC = "output-topic";

    private TopologyTestDriver testDriver;
    private TestInputTopic<Long, TwitterAvroModel> inputTopic;
    private TestOutputTopic<String, TwitterAnalyticsAvroModel> outputTopic;

    @BeforeEach
    void setUp() {
        KafkaStreamsConfigData kafkaStreamsConfigData = new KafkaStreamsConfigData();
        kafkaStreamsConfigData.setApplicationID("kafka-streams-runner-test-app");
        kafkaStreamsConfigData.setInputTopicName(INPUT_TOPIC);
        kafkaStreamsConfigData.setOutputTopicName(OUTPUT_TOPIC);
        kafkaStreamsConfigData.setWordCountStoreName("word-count-store-test");

        KafkaConfigData kafkaConfigData = new KafkaConfigData();
        kafkaConfigData.setSchemaRegistryUrlKey("schema.registry.url");
        kafkaConfigData.setSchemaRegistryUrl(SCHEMA_REGISTRY_URL);

        KafkaStreamsRunner runner = new KafkaStreamsRunner(kafkaStreamsConfigData, kafkaConfigData, new Properties());
        Topology topology = runner.buildTopology().build();

        Properties driverConfig = new Properties();
        driverConfig.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-runner-test-app");
        driverConfig.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
        driverConfig.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        driverConfig.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        testDriver = new TopologyTestDriver(topology, driverConfig);

        Map<String, String> serdeConfig = Map.of("schema.registry.url", SCHEMA_REGISTRY_URL);

        SpecificAvroSerde<TwitterAvroModel> inputValueSerde = new SpecificAvroSerde<>();
        inputValueSerde.configure(serdeConfig, false);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, Serdes.Long().serializer(), inputValueSerde.serializer());

        SpecificAvroSerde<TwitterAnalyticsAvroModel> outputValueSerde = new SpecificAvroSerde<>();
        outputValueSerde.configure(serdeConfig, false);
        outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, Serdes.String().deserializer(), outputValueSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        testDriver.close();
    }

    @Test
    void topology_countsWordOccurrencesAcrossTweets() {
        inputTopic.pipeInput(1L, tweet(1L, "hello hello world"));

        List<KeyValue<String, TwitterAnalyticsAvroModel>> emitted = outputTopic.readKeyValuesToList();

        assertThat(emitted).extracting(kv -> kv.key).containsExactly("hello", "hello", "world");
        assertThat(emitted).extracting(kv -> kv.value.getWordCount()).containsExactly(1L, 2L, 1L);
    }

    @Test
    void topology_accumulatesCountsAcrossMultipleTweets() {
        inputTopic.pipeInput(1L, tweet(1L, "hello world"));
        inputTopic.pipeInput(2L, tweet(2L, "hello there"));

        List<KeyValue<String, TwitterAnalyticsAvroModel>> emitted = outputTopic.readKeyValuesToList();
        long finalHelloCount = emitted.stream()
                .filter(kv -> kv.key.equals("hello"))
                .reduce((first, second) -> second)
                .orElseThrow()
                .value.getWordCount();

        assertThat(finalHelloCount).isEqualTo(2L);
    }

    private TwitterAvroModel tweet(long id, String text) {
        return TwitterAvroModel.newBuilder()
                .setId(id)
                .setUserId(id)
                .setText(text)
                .setCreatedAt(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .build();
    }
}
