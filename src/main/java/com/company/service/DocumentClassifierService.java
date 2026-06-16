package com.company.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class DocumentClassifierService {

    @Value("${groq.api.key}")
    private String apiKey;

    public boolean isBill(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String lower = text.toLowerCase();
        boolean hasKeywords = lower.contains("invoice") || lower.contains("receipt")
                || lower.contains("bill") || lower.contains("gst")
                || lower.contains("tax invoice") || lower.contains("total amount")
                || lower.contains("subtotal") || lower.contains("payment due")
                || lower.contains("amount due") || lower.contains("purchase order");
        if (hasKeywords) return true;
        return classifyDocumentType(text) == null;
    }

    public String classifyDocumentType(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "No content found in the uploaded file.";
        }

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.groq.com/openai/v1/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String prompt = "You are a document type classifier.\n\n"
                + "Read the text below and identify what type of document it is.\n\n"
                + "Respond with ONLY one of these exact words:\n"
                + "BILL - if it is a bill, invoice, receipt, or financial transaction\n"
                + "RESUME - if it is a resume or CV\n"
                + "CONTRACT - if it is a legal contract\n"
                + "REPORT - if it is a report, article, or research paper\n"
                + "ID_CARD - if it is an ID or passport\n"
                + "LETTER - if it is a letter or email\n"
                + "OTHER - if it is something else\n\n"
                + "DOCUMENT TEXT:\n"
                + text.substring(0, Math.min(text.length(), 2000));

            Map<String, Object> requestBody = Map.of(
                    "model", "llama3-8b-8192",
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You classify documents. Reply with only one word from the given list."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0,
                    "max_tokens", 10
            );

            System.out.println("=== CALLING GROQ AI for document classification ===");

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("=== GROQ CLASSIFY RESPONSE: " + response + " ===");

            if (response == null) return null;
            String upper = response.toUpperCase();

            if (upper.contains("BILL"))     return null; // it IS a bill
            if (upper.contains("RESUME"))   return "This document appears to be a Resume/CV, not a bill.";
            if (upper.contains("CONTRACT")) return "This document appears to be a Legal Contract, not a bill.";
            if (upper.contains("REPORT"))   return "This document appears to be a Report or Article, not a bill.";
            if (upper.contains("ID_CARD"))  return "This document appears to be an ID/Passport, not a bill.";
            if (upper.contains("LETTER"))   return "This document appears to be a Letter or Email, not a bill.";
            if (upper.contains("OTHER"))    return "This document does not appear to be a bill or receipt.";

            return null;

        } catch (Exception e) {
            System.err.println("=== DocumentClassifierService ERROR: " + e.getMessage() + " ===");
            return null;
        }
    }
}
