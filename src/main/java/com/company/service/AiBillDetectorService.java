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
            System.out.println("=== GROQ KEY LENGTH: " + apiKey.length() + " ===");
            WebClient client = WebClient.builder()
                    .baseUrl("https://api.groq.com/openai/v1/chat/completions")
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            Map<String, Object> body = Map.of(
                "model", "llama3-8b-8192",
                "messages", List.of(
                    Map.of("role", "system", "content", "You are a forensic invoice analyst. Reply only YES or NO."),
                    Map.of("role", "user", "content",
                        "Is this document AI-generated or fake? Signs: generic names, perfect formatting, fake addresses.\nReply YES if AI-generated, NO if real.\n\nTEXT:\n"
                        + text.substring(0, Math.min(text.length(), 2000)))
                ),
                "temperature", 0,
                "max_tokens", 5
            );

            String response = client.post().bodyValue(body).retrieve().bodyToMono(String.class).block();
            System.out.println("=== GROQ AI-DETECT RESPONSE: " + response + " ===");
            return response != null && response.toUpperCase().contains("YES");
        } catch (Exception e) {
            System.err.println("=== AiBillDetectorService ERROR: " + e.getMessage() + " ===");
            return false;
        }
    }
}
