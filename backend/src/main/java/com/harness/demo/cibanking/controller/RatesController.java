package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.service.RatesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes FX rates sourced from the downstream service. Used by the DEV
 * integration gate to prove the cross-service call works end to end.
 */
@RestController
@RequestMapping("/api/rates")
public class RatesController {

    private final RatesService ratesService;

    public RatesController(RatesService ratesService) {
        this.ratesService = ratesService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRates() {
        Map<String, Object> result = ratesService.getRates();
        // If the downstream integration is unavailable, surface 502 so a broken
        // integration fails the DEV gate loudly instead of returning a false 200.
        if (!"ok".equals(result.get("integration"))) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
