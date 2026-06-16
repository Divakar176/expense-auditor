package com.company.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AiBillDetectorService {

    @Value("${groq.api.key}")
    private String apiKey;

    public boolean isAiGenerated(String text) {
        if (text == null || text.trim().isEmpty()) return false;

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://api.groq.com/openai/v1/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String prompt = "You are a forensic document expert.\n\n"
                + "Analyze the bill/invoice text below and determine whether it was:\n"
                + "- Created by an AI tool (ChatGPT, Claude, fake invoice generator)\n"
                + "- OR is a real scanned/photographed invoice from an actual transaction\n\n"
                + "Signs of AI-generated bills:\n"
                + "- Perfect formatting with no irregularities\n"
                + "- Generic vendor names like ABC Company, XYZ Services\n"
                + "- Placeholder addresses like 123 Main Street\n"
                + "- Missing or fake GST/tax numbers\n\n"
                + "Respond with only one word: YES or NO\n\n"
                + "BILL TEXT:\n"
                + text.substring(0, Math.min(text.length(), 2000));

            Map<String, Object> requestBody = Map.of(
                    "model", "llama3-8b-8192",
                    "messages", List.of(
                            Map.of("role", "system", "content",
                                    "You are a forensic invoice analyst. Reply only YES or NO."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0,
                    "max_tokens", 5
            );

            System.out.println("=== CALLING GROQ AI for AI-generated check ===");

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("=== GROQ RESPONSE: " + response + " ===");

            if (response == null) return false;
            String upper = response.toUpperCase();
            return upper.contains("\"YES\"") || upper.contains(": YES") || upper.contains("YES");

        } catch (Exception e) {
            System.err.println("=== AiBillDetectorService ERROR: " + e.getMessage() + " ===");
            return false;
        }
    }
}
