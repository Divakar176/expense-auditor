package com.company.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiBillDetectorService {

    @Value("${openai.api.key}")
    private String apiKey;

    public boolean isAiGenerated(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.openai.com/v1/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String prompt = """
                You are a forensic document expert specializing in detecting AI-generated invoices and receipts.

                Analyze the bill/invoice text below and determine whether it was:
                - Created by an AI tool (e.g. ChatGPT, Claude, Midjourney, a fake invoice generator)
                - OR is a real scanned/photographed invoice from an actual transaction

                Signs of AI-generated bills:
                - Perfect formatting with no irregularities
                - Suspiciously round numbers
                - Generic vendor names like "ABC Company", "XYZ Services", "Sample Store"
                - Placeholder-style addresses ("123 Main Street", "City, State 12345")
                - Missing or fake GST/tax numbers
                - No realistic transaction reference numbers
                - Too clean/structured for a real receipt

                Respond ONLY with one word:
                YES = AI generated bill
                NO = Real bill

                BILL TEXT:
                """ + text.substring(0, Math.min(text.length(), 3000));

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a forensic invoice analyst. Reply only YES or NO."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0,
                    "max_tokens", 5
            );

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response != null && response.toUpperCase().contains("\"YES\"");

        } catch (Exception e) {
            System.err.println("AiBillDetectorService error: " + e.getMessage());
            return false;
        }
    }
}
