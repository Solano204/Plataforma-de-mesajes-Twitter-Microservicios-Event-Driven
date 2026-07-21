package com.microservices.demo.elastic.query.service.port.out;

import com.microservices.demo.elastic.query.service.QueryType;

/**
 * Output port for retrieving a word's occurrence count from whichever backend
 * is currently configured (Kafka Streams state store or the analytics database).
 * The application layer depends on this abstraction only - it has no knowledge
 * of WebClient or any other transport mechanism used by the adapter.
 */
public interface WordCountQueryPort {

    Long getWordCount(QueryType queryType, String text, String accessToken);
}
