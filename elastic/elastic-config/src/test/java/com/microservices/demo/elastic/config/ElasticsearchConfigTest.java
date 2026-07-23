package com.microservices.demo.elastic.config;

import com.microservices.demo.config.ElasticConfigData;
import org.junit.jupiter.api.Test;
import org.springframework.data.elasticsearch.client.ClientConfiguration;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchConfigTest {

    @Test
    void clientConfiguration_buildsItFromTheConfiguredConnectionUrlAndTimeouts() {
        ElasticConfigData configData = new ElasticConfigData();
        configData.setConnectionUrl("localhost:9200");
        configData.setConnectTimeoutMs(3000);
        configData.setSocketTimeoutMs(5000);

        ClientConfiguration clientConfiguration = new ElasticsearchConfig(configData).clientConfiguration();

        assertThat(clientConfiguration.getEndpoints()).containsExactly(InetSocketAddress.createUnresolved("localhost", 9200));
        assertThat(clientConfiguration.getConnectTimeout()).isEqualTo(Duration.ofMillis(3000));
        assertThat(clientConfiguration.getSocketTimeout()).isEqualTo(Duration.ofMillis(5000));
    }
}
