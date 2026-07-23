package com.microservices.demo.twitter.to.kafka.service.transformer;

import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import twitter4j.Status;
import twitter4j.User;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TwitterStatusToAvroTransformerTest {

    private final TwitterStatusToAvroTransformer transformer = new TwitterStatusToAvroTransformer();

    @Mock
    private Status status;

    @Mock
    private User user;

    @Test
    void getTwitterAvroModelFromStatus_mapsEveryFieldFromTheTwitterContract() {
        Date createdAt = new Date();
        when(status.getId()).thenReturn(123L);
        when(status.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(456L);
        when(status.getText()).thenReturn("hello world");
        when(status.getCreatedAt()).thenReturn(createdAt);

        TwitterAvroModel result = transformer.getTwitterAvroModelFromStatus(status);

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getUserId()).isEqualTo(456L);
        assertThat(result.getText()).isEqualTo("hello world");
        assertThat(result.getCreatedAt()).isEqualTo(createdAt.getTime());
    }
}
