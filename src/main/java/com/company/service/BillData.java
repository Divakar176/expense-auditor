package com.company.service;

public class BillData {
    public final String amount;
    public final String date;
    public final String vendor;
    public final String flightNumber;
    public final String gstNumber;
    public final boolean gstValid;

    public BillData(String amount, String date, String vendor,
                    String flightNumber, String gstNumber, boolean gstValid) {
        this.amount = amount;
        this.date = date;
        this.vendor = vendor;
        this.flightNumber = flightNumber;
        this.gstNumber = gstNumber;
        this.gstValid = gstValid;
    }
}