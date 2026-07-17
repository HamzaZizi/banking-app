package com.harness.demo.cibanking.model;

import java.math.BigDecimal;

public class Mortgage {

    private String id;
    private String product;
    private BigDecimal outstandingBalance;
    private BigDecimal originalAmount;
    private BigDecimal interestRate;
    private String rateType;
    private String rateEndDate;
    private BigDecimal monthlyPayment;
    private String nextPaymentDate;
    private int termRemainingMonths;

    public Mortgage() {
    }

    public Mortgage(String id, String product, BigDecimal outstandingBalance, BigDecimal originalAmount,
                     BigDecimal interestRate, String rateType, String rateEndDate, BigDecimal monthlyPayment,
                     String nextPaymentDate, int termRemainingMonths) {
        this.id = id;
        this.product = product;
        this.outstandingBalance = outstandingBalance;
        this.originalAmount = originalAmount;
        this.interestRate = interestRate;
        this.rateType = rateType;
        this.rateEndDate = rateEndDate;
        this.monthlyPayment = monthlyPayment;
        this.nextPaymentDate = nextPaymentDate;
        this.termRemainingMonths = termRemainingMonths;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public String getRateType() {
        return rateType;
    }

    public void setRateType(String rateType) {
        this.rateType = rateType;
    }

    public String getRateEndDate() {
        return rateEndDate;
    }

    public void setRateEndDate(String rateEndDate) {
        this.rateEndDate = rateEndDate;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public void setMonthlyPayment(BigDecimal monthlyPayment) {
        this.monthlyPayment = monthlyPayment;
    }

    public String getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(String nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public int getTermRemainingMonths() {
        return termRemainingMonths;
    }

    public void setTermRemainingMonths(int termRemainingMonths) {
        this.termRemainingMonths = termRemainingMonths;
    }
}
