package com.harness.demo.cibanking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the HTTP client used to call the downstream Payments Fraud Check
 * service. The base URL is injected from DOWNSTREAM_URL so it can differ per
 * environment (dev/sit/staging/prod each point at their own fraud-check service).
 */
@Configuration
public class DownstreamConfig {

    @Value("${downstream.fraudcheck.url:http://localhost:9090}")
    private String fraudCheckBaseUrl;

    @Bean
    public RestClient fraudRestClient() {
        return RestClient.builder()
                .baseUrl(fraudCheckBaseUrl)
                .build();
    }
}
