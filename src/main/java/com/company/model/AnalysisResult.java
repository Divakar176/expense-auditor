package com.company.model;


public class AnalysisResult {

    private String status;
    private int confidence;
    private String reason;
    private String riskLevel;

    // Bill Information
    private String vendorName;
    private Double amount;
    private String billDate;
    private String invoiceNo;
    private String fileHash;

    // Default Constructor
    public AnalysisResult() {
        this.vendorName = "Unknown Vendor";
        this.amount = 0.0;
        this.billDate = "Unknown Date";
        this.invoiceNo = "N/A";
    }

    // Main Constructor
    public AnalysisResult(String status, int confidence,
                          String reason, String riskLevel) {

        this.status = status;
        this.confidence = confidence;
        this.reason = reason;
        this.riskLevel = riskLevel;

        this.vendorName = "Unknown Vendor";
        this.amount = 0.0;
        this.billDate = "Unknown Date";
        this.invoiceNo = "N/A";
    }

    // =========================
    // Getters
    // =========================

    public String getStatus() {
        return status;
    }

    public int getConfidence() {
        return confidence;
    }

    public String getReason() {
        return reason;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getVendorName() {
        return vendorName;
    }

    public Double getAmount() {
        return amount;
    }

    public String getBillDate() {
        return billDate;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getFileHash() {
        return fileHash;
    }

    // Compatibility methods
    public String getVerdict() {
        return status;
    }

    public int getRiskScore() {
        return confidence;
    }

    // =========================
    // Setters
    // =========================

    public void setStatus(String status) {
        this.status = status;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setBillDate(String billDate) {
        this.billDate = billDate;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    // Compatibility methods
    public void setVerdict(String verdict) {
        this.status = verdict;
    }

    public void setRiskScore(int riskScore) {
        this.confidence = riskScore;
    }

    @Override
    public String toString() {
        return "AnalysisResult{" +
                "status='" + status + '\'' +
                ", confidence=" + confidence +
                ", reason='" + reason + '\'' +
                ", riskLevel='" + riskLevel + '\'' +
                ", vendorName='" + vendorName + '\'' +
                ", amount=" + amount +
                ", billDate='" + billDate + '\'' +
                ", invoiceNo='" + invoiceNo + '\'' +
                ", fileHash='" + fileHash + '\'' +
                '}';
    }
}