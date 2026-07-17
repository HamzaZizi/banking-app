package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.service.FraudCheckService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes the payments fraud-check result sourced from the downstream service.
 * Used by the DEV integration gate to prove the cross-service call works end to end.
 */
@RestController
@RequestMapping("/api/fraud-check")
public class FraudCheckController {

    private final FraudCheckService fraudCheckService;

    public FraudCheckController(FraudCheckService fraudCheckService) {
        this.fraudCheckService = fraudCheckService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> check() {
        Map<String, Object> result = fraudCheckService.check();
        // If the downstream integration is unavailable, surface 502 so a broken
        // integration fails the DEV gate loudly instead of returning a false 200.
        if (!"ok".equals(result.get("integration"))) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(result);
        }
        return ResponseEntity.ok(result);
    }
}
