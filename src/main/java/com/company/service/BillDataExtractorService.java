package com.company.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class BillDataExtractorService {

    public BillData extract(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return new BillData("0.00", "Unknown Date", "Unknown Vendor", "Unknown Flight", "NOT FOUND", false);
        }

        String vendor = "Unknown Vendor";
        String date = "Unknown Date";
        String amount = "0.00";
        String flightNumber = "Unknown Flight";
        String gstNumber = "NOT FOUND";
        boolean gstValid = false;

        String lowerText = extractedText.toLowerCase();

        // Flight detection
        boolean isTravelDoc = lowerText.contains("flight")    ||
                              lowerText.contains("pnr")       ||
                              lowerText.contains("airline")   ||
                              lowerText.contains("passenger") ||
                              lowerText.contains("boarding")  ||
                              lowerText.contains("ticket");

        if (isTravelDoc) {
            Pattern flightPattern = Pattern.compile(
                "(?i)\\b(6E|AI|UK|SG|G8|QP|I5)\\s*[-─]?\\s*(\\d{3,4})\\b");
            Matcher flightMatcher = flightPattern.matcher(extractedText);
            if (flightMatcher.find()) {
                flightNumber = flightMatcher.group(1).toUpperCase() + flightMatcher.group(2);
                vendor = flightMatcher.group(1).toUpperCase() + " Airlines";
            }
        }

        // First line vendor fallback
        String[] lines = extractedText.split("\\r?\\n");
        for (String line : lines) {
            String clean = line.trim();
            if (!clean.isEmpty() &&
                !clean.toLowerCase().contains("invoice") &&
                !clean.toLowerCase().contains("welcome") &&
                clean.matches(".*[a-zA-Z]{3,}.*") &&
                "Unknown Vendor".equals(vendor)) {
                vendor = clean;
                break;
            }
        }

        // Known vendor overrides
        if (lowerText.contains("walmart"))      vendor = "Walmart Supercenter";
        if (lowerText.contains("zudio"))        vendor = "Zudio";
        if (lowerText.contains("royal inn"))    vendor = "Royal Inn Stay";
        if (lowerText.contains("phantom test")) vendor = "Phantom Test Hotel Resort";
        if (lowerText.contains("uber"))         vendor = "Uber India Technologies";
        if (lowerText.contains("ola"))          vendor = "Ola Cabs";
        if (lowerText.contains("ranjith"))      vendor = "Ranjith Enterprises";

        // Amount extraction
        Pattern amountPattern = Pattern.compile(
            "(?:grand\\s+total|total\\s+amount|total|amount|due|payable|rs\\.?|inr)" +
            "[:\\s\\-\u2013₹$]*([\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher amountMatcher = amountPattern.matcher(extractedText);
        String lastAmount = null;
        while (amountMatcher.find()) {
            String found = amountMatcher.group(1).trim();
            if (!found.isEmpty()) lastAmount = found;
        }
        if (lastAmount != null) amount = lastAmount;

        // Date extraction — labelled first
        Pattern labelledDate = Pattern.compile(
            "(?:date|dt|dated)[\\s:.]*" +
            "(\\d{1,2}[./\\-]\\d{1,2}[./\\-]\\d{2,4})",
            Pattern.CASE_INSENSITIVE
        );
        Matcher labelledMatcher = labelledDate.matcher(extractedText);
        if (labelledMatcher.find()) {
            date = labelledMatcher.group(1).trim();
        } else {
            Pattern bareDatePattern = Pattern.compile(
                "(\\d{4}[-/.]\\d{2}[-/.]\\d{2}" +
                "|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{4}" +
                "|\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2})"
            );
            Matcher bareMatcher = bareDatePattern.matcher(extractedText);
            if (bareMatcher.find()) date = bareMatcher.group(1).trim();
        }

        // GST extraction and validation
        Pattern gstPattern = Pattern.compile(
            "\\b(\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}[A-Z\\d]{1}Z[A-Z\\d]{1})\\b",
            Pattern.CASE_INSENSITIVE
        );
        Matcher gstMatcher = gstPattern.matcher(extractedText);
        if (gstMatcher.find()) {
            gstNumber = gstMatcher.group(1).toUpperCase();
            gstValid  = validateGst(gstNumber);
            System.out.println("✅ GST found: " + gstNumber + " valid=" + gstValid);
        }

        return new BillData(amount, date, vendor, flightNumber, gstNumber, gstValid);
    }

    private boolean validateGst(String gst) {
        if (gst == null || gst.length() != 15) return false;
        try {
            int stateCode = Integer.parseInt(gst.substring(0, 2));
            if (stateCode < 1 || stateCode > 37) return false;
        } catch (NumberFormatException e) {
            return false;
        }
        if (!gst.substring(2, 7).matches("[A-Z]{5}"))  return false;
        if (!gst.substring(7, 11).matches("\\d{4}"))   return false;
        if (!gst.substring(11, 12).matches("[A-Z]"))   return false;
        if (gst.charAt(13) != 'Z')                     return false;
        return true;
    }
}