package com.microservices.demo.elastic.query.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
@EnableDiscoveryClient
@SpringBootApplication
// Remove @EnableElasticsearchRepositories from here
@ComponentScan(basePackages = "com.microservices.demo")
public class ElasticQueryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ElasticQueryServiceApplication.class, args);
    }
}

//git push origin main.  
// git commit -m "Update README to refresh last updated date" git push origin main.  