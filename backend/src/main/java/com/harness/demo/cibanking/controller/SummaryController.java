package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.service.BankingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final BankingService bankingService;

    public SummaryController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping
    public Map<String, Object> getSummary() {
        return bankingService.getSummary();
    }
}
