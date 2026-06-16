package com.company.service;

import org.springframework.stereotype.Service;

@Service
public class DocumentClassifierService {

    public boolean isBill(String text) {

        if (text == null || text.isEmpty()) {
            return false;
        }

        text = text.toLowerCase();

        return text.contains("invoice")
                || text.contains("receipt")
                || text.contains("bill")
                || text.contains("gst")
                || text.contains("tax invoice")
                || text.contains("total amount");
    }
}