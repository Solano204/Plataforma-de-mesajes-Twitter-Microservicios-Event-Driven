package com.microservices.demo.elastic.query.service.adapter.out.client;

import com.microservices.demo.config.ElasticQueryServiceConfigData;
import com.microservices.demo.elastic.query.service.QueryType;
import com.microservices.demo.elastic.query.service.common.exception.ElasticQueryServiceException;
import com.microservices.demo.elastic.query.service.model.ElasticQueryServiceWordCountResponseModel;
import com.microservices.demo.elastic.query.service.port.out.WordCountQueryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static com.microservices.demo.mdc.Constants.CORRELATION_ID_HEADER;
import static com.microservices.demo.mdc.Constants.CORRELATION_ID_KEY;

@Component
public class WebClientWordCountAdapter implements WordCountQueryPort {

    private static final Logger LOG = LoggerFactory.getLogger(WebClientWordCountAdapter.class);

    private final ElasticQueryServiceConfigData elasticQueryServiceConfigData;

    private final WebClient.Builder webClientBuilder;

    public WebClientWordCountAdapter(ElasticQueryServiceConfigData queryServiceConfigData,
                                      @Qualifier("webClientBuilder") WebClient.Builder clientBuilder) {
        this.elasticQueryServiceConfigData = queryServiceConfigData;
        this.webClientBuilder = clientBuilder;
    }

    @Override
    public Long getWordCount(QueryType queryType, String text, String accessToken) {
        ElasticQueryServiceConfigData.Query query = queryType == QueryType.KAFKA_STATE_STORE
                ? elasticQueryServiceConfigData.getQueryFromKafkaStateStore()
                : elasticQueryServiceConfigData.getQueryFromAnalyticsDatabase();
        LOG.info("Querying word count for text {} via {}", text, queryType);
        return retrieveResponseModel(text, accessToken, query).getWordCount();
    }

    private ElasticQueryServiceWordCountResponseModel retrieveResponseModel(String text,
                                                                            String accessToken,
                                                                            ElasticQueryServiceConfigData.Query query) {
        return webClientBuilder
                .build()
                .method(HttpMethod.valueOf(query.getMethod()))
                .uri(query.getUri(), uriBuilder -> uriBuilder.build(text))
                .headers(h -> {
                    h.setBearerAuth(accessToken);
                    h.set(CORRELATION_ID_HEADER, MDC.get(CORRELATION_ID_KEY));
                })
                .accept(MediaType.valueOf(query.getAccept()))
                .retrieve()
                .onStatus(
                        s -> s.equals(HttpStatus.UNAUTHORIZED),
                        clientResponse -> Mono.just(new BadCredentialsException("Not authenticated")))
                .onStatus(
                        s -> s.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> Mono.just(new
                                ElasticQueryServiceException(clientResponse.statusCode().toString())))
                .onStatus(
                        s -> s.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> Mono.just(new Exception(clientResponse.statusCode().toString())))
                .bodyToMono(ElasticQueryServiceWordCountResponseModel.class)
                .log()
                .block();
    }
}
