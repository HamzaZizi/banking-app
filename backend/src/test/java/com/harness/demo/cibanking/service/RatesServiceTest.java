package com.harness.demo.cibanking.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the downstream FX Rates integration. A MockWebServer stands in
 * for the real rates-service so we can exercise both the healthy and the
 * unavailable paths without a live dependency.
 */
class RatesServiceTest {

    private MockWebServer server;
    private RatesService service;

    @BeforeEach
    void setUp() throws java.io.IOException {
        server = new MockWebServer();
        server.start();
        RestClient client = RestClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        service = new RatesService(client);
    }

    @AfterEach
    void tearDown() {
        // shutdown() can throw a benign IO cleanup error on some runners;
        // it's only releasing the mock server, so don't fail the test over it.
        try {
            server.shutdown();
        } catch (Exception ignored) {
            // no-op
        }
    }

    @Test
    void getRates_returnsOkWhenDownstreamResponds() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"base\":\"GBP\",\"rates\":{\"USD\":1.28}}"));

        Map<String, Object> result = service.getRates();

        assertThat(result.get("integration")).isEqualTo("ok");
        assertThat(result.get("source")).isEqualTo("downstream");
        assertThat(result).containsKey("rates");
    }

    @Test
    void getRates_returnsUnavailableWhenDownstreamErrors() {
        server.enqueue(new MockResponse().setResponseCode(500));

        Map<String, Object> result = service.getRates();

        assertThat(result.get("integration")).isEqualTo("unavailable");
        assertThat(result).containsKey("error");
    }
}
