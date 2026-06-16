package com.company.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNo;
    private String vendorName;
    private Double amount;
    private String billDate;
    private String verdict;
    private int riskScore;
    private String fileHash;

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileHash() {
        return fileHash;
    }

    // Default Constructor
    public Bill() {
    }

    // Parameterized Constructor
    public Bill(String invoiceNo, String vendorName, Double amount,
                String billDate, String verdict, int riskScore) {
        this.invoiceNo = invoiceNo;
        this.vendorName = vendorName;
        this.amount = amount;
        this.billDate = billDate;
        this.verdict = verdict;
        this.riskScore = riskScore;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getBillDate() {
        return billDate;
    }

    public void setBillDate(String billDate) {
        this.billDate = billDate;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    @Override
    public String toString() {
        return "Bill{" +
                "id=" + id +
                ", invoiceNo='" + invoiceNo + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", amount=" + amount +
                ", billDate='" + billDate + '\'' +
                ", verdict='" + verdict + '\'' +
                ", riskScore=" + riskScore +
                '}';
    }
}