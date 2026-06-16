package com.company;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.company.service.BillData;
import com.company.service.BillDataExtractorService; // FIX: Changed from BillDataExtractorService.BillData

public class BillAnalysisServiceTest {

    private final BillDataExtractorService extractor = new BillDataExtractorService();

    @Test
    public void testZudioVendorDetected() {
        String text = "ZUDIO\nTotal Amount: 2799\nDate: 2024-01-15";
        BillData data = extractor.extract(text);
        assertEquals("Zudio", data.vendor);
    }

    @Test
    public void testAmountExtraction() {
        String text = "Thank you\nTotal: 1500.00\nDate: 2024-02-20";
        BillData data = extractor.extract(text);
        assertEquals("1500.00", data.amount);
    }

    @Test
    public void testFutureDateExtracted() {
        String text = "Vendor: TestShop\nTotal: 500\nDate: 2099-01-01";
        BillData data = extractor.extract(text);
        assertEquals("2099-01-01", data.date);
    }

    @Test
    public void testUberVendorDetected() {
        String text = "Uber India\nTotal: 250.00\nDate: 2024-03-10";
        BillData data = extractor.extract(text);
        assertEquals("Uber India Technologies", data.vendor);
    }
}