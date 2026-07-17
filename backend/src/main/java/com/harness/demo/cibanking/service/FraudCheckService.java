package com.harness.demo.cibanking.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls the downstream Payments Fraud Check service. This is the app's
 * "immediate integration": DEV integration tests assert that this cross-service
 * call works after a patch.
 */
@Service
public class FraudCheckService {

    private final RestClient fraudRestClient;

    public FraudCheckService(RestClient fraudRestClient) {
        this.fraudRestClient = fraudRestClient;
    }

    /**
     * Calls the downstream fraud-check service and wraps the response with the
     * integration status, so callers can see whether the dependency is reachable.
     */
    public Map<String, Object> check() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> downstream = fraudRestClient.get()
                    .uri("/fraud-check")
                    .retrieve()
                    .body(Map.class);

            result.put("source", "downstream");
            result.put("integration", "ok");
            result.put("result", downstream);
        } catch (Exception e) {
            result.put("source", "downstream");
            result.put("integration", "unavailable");
            result.put("error", e.getClass().getSimpleName());
        }
        return result;
    }
}
