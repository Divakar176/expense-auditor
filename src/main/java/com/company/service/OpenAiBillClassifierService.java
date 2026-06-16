package com.company.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class AiBillDetectorService {

    private final String API_KEY = "YOUR_OPENAI_KEY";

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1/chat/completions")
            .defaultHeader("Authorization", "Bearer " + API_KEY)
            .defaultHeader("Content-Type", "application/json")
            .build();

    public boolean isAiGenerated(String text) {

        try {
            String prompt = """
            You are a forensic invoice detection AI.

            Decide if the document is AI-generated or real invoice.

            Return only one word:
            YES = AI generated invoice
            NO = real invoice

            TEXT:
            """ + text;

            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini",
                    "messages", new Object[]{
                            Map.of("role", "user", "content", prompt)
                    },
                    "temperature", 0
            );

            String response = webClient.post()
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return response != null && response.contains("YES");

        } catch (Exception e) {
            return false;
        }
    }
}