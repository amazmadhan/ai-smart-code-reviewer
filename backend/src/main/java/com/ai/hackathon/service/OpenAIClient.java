package com.ai.hackathon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Component
public class OpenAIClient {

    @Value("${openai.api.key:}")
    private String openAiKeyFromProps;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private String apiKey() {
        if (openAiKeyFromProps != null && !openAiKeyFromProps.isBlank()) {
            return openAiKeyFromProps;
        }
        return Optional.ofNullable(System.getenv("OPENAI_API_KEY")).orElse("");
    }

    public String askModel(String prompt) {
        try {
            String apiKey = apiKey();
            if (apiKey.isEmpty()) {
                return "OpenAI API key not configured";
            }

            // Create the JSON request body
            String requestBody = String.format(
                    "{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":1000,\"temperature\":0.7}",
                    prompt.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
            );

            // Build the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // Send the request
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                debugResponse(response.body()); // Add this line for debugging
                return extractContentFromResponse(response.body());
            } else {
                return "Error: HTTP " + response.statusCode() + " - " + response.body();
            }

        } catch (Exception e) {
            return "Error processing request: " + e.getMessage();
        }
    }

    private String extractContentFromResponse(String jsonResponse) {
        try {
            // Use regex to find the content field
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "\"content\":\\s*\"(.*?)\"(?=\\s*,\\s*\"refusal\"|\\s*})",
                    java.util.regex.Pattern.DOTALL
            );

            java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);

            if (matcher.find()) {
                String content = matcher.group(1);

                // Unescape JSON string
                content = content.replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t");

                return content;
            }

            return "Unable to extract content from AI response";

        } catch (Exception e) {
            System.err.println("Error extracting content: " + e.getMessage());
            e.printStackTrace();
            return "Error processing AI response: " + e.getMessage();
        }
    }

    private void debugResponse(String response) {
        System.out.println("=== DEBUG: Full OpenAI Response ===");
        System.out.println(response.substring(0, Math.min(500, response.length())));
        System.out.println("=== END DEBUG ===");
    }
}
