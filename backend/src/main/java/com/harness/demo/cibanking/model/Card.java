package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

/**
 * A debit or credit card linked to an account. Dummy demo data only.
 */
public class Card {

    private String id;
    private String accountId;
    private String cardholder;
    private String type;          // DEBIT or CREDIT
    private String network;       // Visa, Mastercard
    private String panMasked;     // e.g. "**** **** **** 4471"
    private String expiry;        // MM/YY
    private String status;        // ACTIVE or FROZEN
    private boolean contactless;
    private BigDecimal monthlySpend;
    private BigDecimal limit;     // credit limit (CREDIT) or daily ATM limit (DEBIT)

    public Card() {
    }

    public Card(String id, String accountId, String cardholder, String type, String network,
                String panMasked, String expiry, String status, boolean contactless,
                BigDecimal monthlySpend, BigDecimal limit) {
        this.id = id;
        this.accountId = accountId;
        this.cardholder = cardholder;
        this.type = type;
        this.network = network;
        this.panMasked = panMasked;
        this.expiry = expiry;
        this.status = status;
        this.contactless = contactless;
        this.monthlySpend = monthlySpend;
        this.limit = limit;
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

    public String getCardholder() {
        return cardholder;
    }

    public void setCardholder(String cardholder) {
        this.cardholder = cardholder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getPanMasked() {
        return panMasked;
    }

    public void setPanMasked(String panMasked) {
        this.panMasked = panMasked;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isContactless() {
        return contactless;
    }

    public void setContactless(boolean contactless) {
        this.contactless = contactless;
    }

    public BigDecimal getMonthlySpend() {
        return monthlySpend;
    }

    public void setMonthlySpend(BigDecimal monthlySpend) {
        this.monthlySpend = monthlySpend;
    }

    public BigDecimal getLimit() {
        return limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }
}
