package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Transaction;
import com.harness.demo.cibanking.service.BankingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final BankingService bankingService;

    public TransactionController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    /**
     * Search / filter transactions across all accounts. All parameters optional.
     * e.g. /api/transactions?query=coffee&category=Eating%20out&type=DEBIT&accountId=acc-001&from=2026-07-01&to=2026-07-31
     */
    @GetMapping
    public List<Transaction> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return bankingService.searchTransactions(query, category, type, accountId, from, to);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return bankingService.getCategories();
    }
}
