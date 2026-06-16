package com.company.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class DocumentClassifierService {

    @Value("${openai.api.key}")
    private String apiKey;

    /**
     * Returns true if the document is a bill/invoice/receipt.
     * Uses keyword check first (fast path), then falls back to OpenAI (smart path).
     */
    public boolean isBill(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        String lower = text.toLowerCase();

        // Fast keyword check — if clear bill keywords exist, no need to call AI
        boolean hasKeywords = lower.contains("invoice") || lower.contains("receipt")
                || lower.contains("bill") || lower.contains("gst")
                || lower.contains("tax invoice") || lower.contains("total amount")
                || lower.contains("subtotal") || lower.contains("payment due")
                || lower.contains("amount due") || lower.contains("purchase order");

        if (hasKeywords) return true;

        // AI fallback — check if document looks like a bill even without clear keywords
        return isDocumentABillByAI(text);
    }

    /**
     * Calls OpenAI to classify if document is a bill/invoice/receipt or something else.
     * Returns "NOT_A_BILL" reason string if it's not a bill, null if it is.
     */
    public String classifyDocumentType(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "EMPTY_DOCUMENT";
        }

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.openai.com/v1/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String prompt = """
                You are a document type classifier.

                Read the text below and identify what type of document it is.

                Respond ONLY with one of these exact labels:
                BILL = It is a bill, invoice, receipt, or financial transaction document
                RESUME = It is a resume or CV
                CONTRACT = It is a legal contract or agreement
                REPORT = It is a report, research paper, or article
                ID_CARD = It is an ID, passport, or identity document
                LETTER = It is a letter or email
                IMAGE_NO_TEXT = The image has no readable text
                OTHER = Something else entirely

                DOCUMENT TEXT:
                """ + text.substring(0, Math.min(text.length(), 2000));

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a document classifier. Reply only with the exact label."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0,
                    "max_tokens", 10
            );

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response == null) return null;

            if (response.contains("BILL")) return null; // It's a valid bill
            if (response.contains("RESUME")) return "This document appears to be a Resume/CV, not a bill.";
            if (response.contains("CONTRACT")) return "This document appears to be a Legal Contract, not a bill.";
            if (response.contains("REPORT")) return "This document appears to be a Report or Article, not a bill.";
            if (response.contains("ID_CARD")) return "This document appears to be an ID/Passport, not a bill.";
            if (response.contains("LETTER")) return "This document appears to be a Letter or Email, not a bill.";
            if (response.contains("IMAGE_NO_TEXT")) return "No readable text found in the uploaded image.";
            if (response.contains("OTHER")) return "This document does not appear to be a bill or receipt.";

            return null; // Unknown — let other checks handle it

        } catch (Exception e) {
            System.err.println("DocumentClassifierService AI error: " + e.getMessage());
            return null; // Don't block on AI error
        }
    }

    private boolean isDocumentABillByAI(String text) {
        String reason = classifyDocumentType(text);
        return reason == null; // null means it IS a bill
    }
}
