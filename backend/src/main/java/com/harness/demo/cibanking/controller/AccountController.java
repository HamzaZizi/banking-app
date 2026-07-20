package com.harness.demo.cibanking.controller;

import com.harness.demo.cibanking.model.Account;
import com.harness.demo.cibanking.model.Transaction;
import com.harness.demo.cibanking.service.BankingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final BankingService bankingService;

    public AccountController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @GetMapping
    public List<Account> getAccounts() {
        return bankingService.getAccounts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        Account account = bankingService.getAccount(id);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String id) {
        Map<String, Object> balance = bankingService.getBalance(id);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String id) {
        List<Transaction> transactions = bankingService.getTransactions(id);
        if (transactions == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}/insights")
    public ResponseEntity<Map<String, Object>> getInsights(@PathVariable String id) {
        Map<String, Object> insights = bankingService.getInsights(id);
        if (insights == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(insights);
    }

    @GetMapping("/{id}/statements")
    public ResponseEntity<List<Map<String, Object>>> getStatements(@PathVariable String id) {
        List<Map<String, Object>> statements = bankingService.getStatements(id);
        if (statements == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statements);
    }

    /** Rename an account. Body: { "nickname": "New name" }. */
    @PutMapping("/{id}/nickname")
    public ResponseEntity<Account> updateNickname(@PathVariable String id,
                                                  @RequestBody Map<String, String> body) {
        String nickname = body.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Account account = bankingService.updateNickname(id, nickname.trim());
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }
}
