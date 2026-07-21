package com.microservices.demo.twitter.to.kafka.service.listener;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.producer.config.service.KafkaProducer;
import com.microservices.demo.twitter.to.kafka.service.transformer.TwitterStatusToAvroTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import twitter4j.Status;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterKafkaStatusListenerTest {

    @Mock
    private KafkaConfigData kafkaConfigData;

    @Mock
    private KafkaProducer<Long, TwitterAvroModel> kafkaProducer;

    @Mock
    private TwitterStatusToAvroTransformer transformer;

    @Mock
    private Status status;

    private TwitterKafkaStatusListener listener;

    @BeforeEach
    void setUp() {
        listener = new TwitterKafkaStatusListener(kafkaConfigData, kafkaProducer, transformer);
    }

    @Test
    void onStatus_transformsAndSendsToTheConfiguredTopicKeyedByUserId() {
        TwitterAvroModel avroModel = TwitterAvroModel.newBuilder()
                .setId(1L).setUserId(99L).setText("hi").setCreatedAt(0L).build();
        when(status.getText()).thenReturn("hi");
        when(kafkaConfigData.getTopicName()).thenReturn("twitter-topic");
        when(transformer.getTwitterAvroModelFromStatus(status)).thenReturn(avroModel);

        listener.onStatus(status);

        verify(kafkaProducer).send(eq("twitter-topic"), eq(99L), eq(avroModel));
    }
}
