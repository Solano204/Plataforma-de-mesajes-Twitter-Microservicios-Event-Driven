package com.microservices.demo.kafka.producer.config.service.impl;

import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// KafkaTemplate.send() is a STUB here (returns a canned, already-completed
// future per test) - the point of these tests is what TwitterKafkaProducer
// does with the outcome (call the right method with the right args, never
// let a failed send throw synchronously), not to verify a specific
// interaction protocol with KafkaTemplate itself.
@ExtendWith(MockitoExtension.class)
class TwitterKafkaProducerTest {

    @Mock
    private KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate;
    @Mock
    private SendResult<Long, TwitterAvroModel> sendResult;
    @Mock
    private RecordMetadata recordMetadata;

    private TwitterAvroModel message() {
        return TwitterAvroModel.newBuilder()
                .setUserId(1L)
                .setId(42L)
                .setText("hello world")
                .setCreatedAt(System.currentTimeMillis())
                .build();
    }

    @Test
    void send_delegatesToKafkaTemplateWithTheExactTopicKeyAndMessage() {
        TwitterAvroModel msg = message();
        when(kafkaTemplate.send("twitter-topic", 1L, msg))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        TwitterKafkaProducer producer = new TwitterKafkaProducer(kafkaTemplate);

        producer.send("twitter-topic", 1L, msg);

        verify(kafkaTemplate).send(eq("twitter-topic"), eq(1L), eq(msg));
    }

    @Test
    void send_kafkaTemplateFutureCompletesSuccessfully_doesNotThrow() {
        when(recordMetadata.topic()).thenReturn("twitter-topic");
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(100L);
        when(recordMetadata.timestamp()).thenReturn(System.currentTimeMillis());
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(kafkaTemplate.send(anyString(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
        TwitterKafkaProducer producer = new TwitterKafkaProducer(kafkaTemplate);

        assertThatCode(() -> producer.send("twitter-topic", 1L, message())).doesNotThrowAnyException();
    }

    @Test
    void send_kafkaTemplateFutureFailsAsynchronously_stillDoesNotThrowSynchronously() {
        CompletableFuture<SendResult<Long, TwitterAvroModel>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("broker unreachable"));
        when(kafkaTemplate.send(anyString(), any(), any())).thenReturn(failedFuture);
        TwitterKafkaProducer producer = new TwitterKafkaProducer(kafkaTemplate);

        // send() is fire-and-forget - a downstream broker failure must be logged
        // asynchronously via the callback, never thrown out of send() itself.
        assertThatCode(() -> producer.send("twitter-topic", 1L, message())).doesNotThrowAnyException();
    }

    @Test
    void close_destroysTheUnderlyingKafkaTemplate() {
        TwitterKafkaProducer producer = new TwitterKafkaProducer(kafkaTemplate);

        producer.close();

        verify(kafkaTemplate).destroy();
    }
}
