package com.microservices.demo.kafka.streams.service.api;

import com.microservices.demo.kafka.streams.service.model.KafkaStreamsResponseModel;
import com.microservices.demo.kafka.streams.service.runner.StreamsRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaStreamsControllerTest {

    @Mock
    private StreamsRunner<String, Long> streamsRunner;

    private KafkaStreamsController controller() {
        return new KafkaStreamsController(streamsRunner);
    }

    @Test
    void getWordCountByWord_wrapsTheRunnersResultInAResponseModel() {
        when(streamsRunner.getValueByKey("hello")).thenReturn(7L);

        ResponseEntity<KafkaStreamsResponseModel> response = controller().getWordCountByWord("hello");

        assertThat(response.getBody().getWord()).isEqualTo("hello");
        assertThat(response.getBody().getWordCount()).isEqualTo(7L);
    }

    @Test
    void getWordCountByWord_unknownWord_returnsANullWordCountNotAnError() {
        // StreamsRunner.getValueByKey defaults to returning null for a
        // missing key (see StreamsRunner's default method) - the controller
        // doesn't special-case that, so it flows straight through as a 200
        // with wordCount: null rather than a 404.
        when(streamsRunner.getValueByKey("missing")).thenReturn(null);

        ResponseEntity<KafkaStreamsResponseModel> response = controller().getWordCountByWord("missing");

        assertThat(response.getBody().getWordCount()).isNull();
    }
}
