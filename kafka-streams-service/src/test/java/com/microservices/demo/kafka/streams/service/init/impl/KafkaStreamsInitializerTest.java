package com.microservices.demo.kafka.streams.service.init.impl;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.admin.client.KafkaAdminClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsInitializerTest {

    @Mock
    private KafkaAdminClient kafkaAdminClient;

    @Test
    void init_checksTopicsCreatedBeforeCheckingSchemaRegistry() {
        KafkaConfigData configData = new KafkaConfigData();
        configData.setTopicNamesToCreate(List.of("twitter-topic"));

        new KafkaStreamsInitializer(configData, kafkaAdminClient).init();

        InOrder order = inOrder(kafkaAdminClient);
        order.verify(kafkaAdminClient).checkTopicsCreated();
        order.verify(kafkaAdminClient).checkSchemaRegistry();
    }
}
