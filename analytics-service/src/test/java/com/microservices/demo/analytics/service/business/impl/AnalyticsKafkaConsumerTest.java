package com.microservices.demo.analytics.service.business.impl;

import com.microservices.demo.analytics.service.dataaccess.entity.AnalyticsEntity;
import com.microservices.demo.analytics.service.dataaccess.repository.AnalyticsRepository;
import com.microservices.demo.analytics.service.transformer.AvroToDbEntityModelTransformer;
import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.admin.client.KafkaAdminClient;
import com.microservices.demo.kafka.avro.model.TwitterAnalyticsAvroModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsKafkaConsumerTest {

    @Mock
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
    @Mock
    private KafkaAdminClient kafkaAdminClient;
    @Mock
    private AvroToDbEntityModelTransformer avroToDbEntityModelTransformer;
    @Mock
    private AnalyticsRepository analyticsRepository;
    @Mock
    private MessageListenerContainer messageListenerContainer;

    private AnalyticsKafkaConsumer consumer() {
        KafkaConfigData configData = new KafkaConfigData();
        configData.setTopicNamesToCreate(List.of("twitter-analytics-topic"));
        return new AnalyticsKafkaConsumer(kafkaListenerEndpointRegistry, kafkaAdminClient, configData,
                avroToDbEntityModelTransformer, analyticsRepository);
    }

    @Test
    void onAppStarted_checksTopicsCreatedThenStartsTheNamedListenerContainer() {
        when(kafkaListenerEndpointRegistry.getListenerContainer("twitterAnalyticsTopicListener"))
                .thenReturn(messageListenerContainer);

        consumer().onAppStarted(null);

        InOrder order = inOrder(kafkaAdminClient, kafkaListenerEndpointRegistry, messageListenerContainer);
        order.verify(kafkaAdminClient).checkTopicsCreated();
        order.verify(kafkaListenerEndpointRegistry).getListenerContainer("twitterAnalyticsTopicListener");
        order.verify(messageListenerContainer).start();
    }

    @Test
    void receive_transformsMessagesThenBatchPersistsTheResultingEntities() {
        TwitterAnalyticsAvroModel avroModel = TwitterAnalyticsAvroModel.newBuilder()
                .setWord("hello").setWordCount(3L).setCreatedAt(1_700_000_000L).build();
        List<TwitterAnalyticsAvroModel> messages = List.of(avroModel);
        List<AnalyticsEntity> entities = List.of(
                new AnalyticsEntity(UUID.randomUUID(), "hello", 3L, LocalDateTime.now()));
        when(avroToDbEntityModelTransformer.getEntityModel(messages)).thenReturn(entities);

        consumer().receive(messages, List.of("key-1"), List.of(0), List.of(100L));

        verify(analyticsRepository).batchPersist(entities);
    }

    @Test
    void receive_emptyMessageBatch_stillCallsBatchPersistWithAnEmptyList() {
        when(avroToDbEntityModelTransformer.getEntityModel(List.of())).thenReturn(List.of());

        consumer().receive(List.of(), List.of(), List.of(), List.of());

        verify(analyticsRepository).batchPersist(eq(List.of()));
    }
}
