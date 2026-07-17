package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Mortgage;
import com.harness.demo.cibanking.service.BankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mortgages")
public class MortgageController {

    private final BankingService bankingService;

    public MortgageController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping
    public List<Mortgage> getMortgages() {
        return bankingService.getMortgages();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Mortgage> getMortgage(@PathVariable String id) {
        Mortgage mortgage = bankingService.getMortgage(id);
        if (mortgage == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mortgage);
    }
}
