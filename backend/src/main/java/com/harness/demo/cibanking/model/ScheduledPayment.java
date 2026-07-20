package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

/**
 * A scheduled outgoing payment: a standing order, a direct debit, or a one-off
 * future-dated payment. Dummy demo data only.
 */
public class ScheduledPayment {

    private String id;
    private String accountId;
    private String payeeName;
    private String kind;        // STANDING_ORDER, DIRECT_DEBIT, SCHEDULED
    private BigDecimal amount;
    private String frequency;   // MONTHLY, WEEKLY, QUARTERLY, ONE_OFF
    private String nextDate;
    private String reference;
    private String status;      // ACTIVE, PAUSED

    public ScheduledPayment() {
    }

    public ScheduledPayment(String id, String accountId, String payeeName, String kind,
                            BigDecimal amount, String frequency, String nextDate,
                            String reference, String status) {
        this.id = id;
        this.accountId = accountId;
        this.payeeName = payeeName;
        this.kind = kind;
        this.amount = amount;
        this.frequency = frequency;
        this.nextDate = nextDate;
        this.reference = reference;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getNextDate() {
        return nextDate;
    }

    public void setNextDate(String nextDate) {
        this.nextDate = nextDate;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
