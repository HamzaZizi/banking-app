package com.harness.demo.cibanking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls the downstream FX Rates service. This is the app's "immediate integration":
 * DEV integration tests assert that this cross-service call works after a patch.
 */
@Service
public class RatesService {

    private final RestClient ratesRestClient;

    public RatesService(RestClient ratesRestClient) {
        this.ratesRestClient = ratesRestClient;
    }

    /**
     * Fetches FX rates from the downstream service and wraps them with the
     * integration status, so callers can see whether the dependency is reachable.
     */
    public Map<String, Object> getRates() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> downstream = ratesRestClient.get()
                    .uri("/rates")
                    .retrieve()
                    .body(Map.class);

            result.put("source", "downstream");
            result.put("integration", "ok");
            result.put("rates", downstream);
        } catch (Exception e) {
            result.put("source", "downstream");
            result.put("integration", "unavailable");
            result.put("error", e.getClass().getSimpleName());
        }
        return result;
    }
}
