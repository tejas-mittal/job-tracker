package com.jobtracker.monolith.email.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for communicating with the Groq API (Llama 3) to classify and extract data from emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.1-8b-instant";

    public record EmailClassificationResult(
            boolean isJobRelated,
            String status,
            String company,
            String role,
            String interviewLink,
            String interviewTime,
            String assessmentDate,
            String notes
    ) {}

    public Optional<EmailClassificationResult> analyzeEmail(String subject, String body) {
        if (this.apiKey == null || this.apiKey.trim().isEmpty()) {
            log.warn("API Key is missing! (Using the 'gemini.api-key' env variable for backwards compatibility)");
            return Optional.empty();
        }

        if ("MOCK".equalsIgnoreCase(this.apiKey.trim())) {
            log.info("Using MOCK AI mode for subject: {}", subject);
            String mockCompany = "MockCompany Inc.";
            if (subject != null && subject.length() > 5) {
                mockCompany = subject.substring(0, Math.min(subject.length(), 15)).replaceAll("[^a-zA-Z0-9 ]", "") + " Corp";
            }
            // Cycle through statuses for variety
            String mockStatus = "APPLIED";
            if (Math.random() > 0.6) mockStatus = "REJECTED";
            else if (Math.random() > 0.8) mockStatus = "INTERVIEW";
            
            EmailClassificationResult mockResult = new EmailClassificationResult(
                    true, mockStatus, mockCompany, "Software Engineer (Mock)", null, null, null, "Mock AI generated entry"
            );
            return Optional.of(mockResult);
        }

        String emailContent = "Subject: " + (subject != null ? subject : "") + "\n\nBody: " + (body != null ? body : "");
        // Truncate email content to ~4000 characters to save tokens and focus on the most important parts
        if (emailContent.length() > 4000) {
            emailContent = emailContent.substring(0, 4000);
        }

        String prompt = """
            You are an expert HR Email Parser. Read the following email and extract the relevant job application details.
            Return a strictly formatted JSON object matching this schema exactly:
            {
              "isJobRelated": boolean, // true ONLY if this email is a job application, interview invite, job rejection, offer, or application withdrawal. False for marketing, newsletters, or unrelated emails.
              "status": string, // MUST be one of: "APPLIED", "INTERVIEW", "REJECTED", "OFFER", "WITHDRAWN".
              "company": string, // The name of the company the user applied to (extract from sender or text. Example: "Google", "Stripe". Return null if unknown).
              "role": string, // The job title (Example: "Software Engineer", "Clerk". Return null if unknown).
              "interviewLink": string, // If this is an interview/assessment, extract any Zoom, Google Meet, Teams, HackerRank, or other meeting/test link. (null if none).
              "interviewTime": string, // Extract the time/date of the interview. (null if none).
              "assessmentDate": string, // Extract the deadline or date for an assessment/test. (null if none).
              "notes": string // Any important notes like Applicant ID, Passwords for tests, or short context. Keep it under 200 chars. (null if none).
            }
            
            Email Content:
            """ + emailContent;

        try {
            // Build the Groq (OpenAI-compatible) API request body
            Map<String, Object> requestBody = Map.of(
                    "model", GROQ_MODEL,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a helpful assistant that only outputs strictly formatted JSON. Do not wrap it in markdown code blocks. Just raw JSON."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object"),
                    "temperature", 0.1
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + this.apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Groq API error ({}): {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            // Parse the OpenAI-compatible response
            var rootNode = objectMapper.readTree(response.body());
            var choices = rootNode.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String jsonText = choices.get(0).path("message").path("content").asText();
                
                // Robustly extract JSON object by finding the first '{' and last '}'
                int startIndex = jsonText.indexOf('{');
                int endIndex = jsonText.lastIndexOf('}');
                
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    jsonText = jsonText.substring(startIndex, endIndex + 1);
                }
                
                EmailClassificationResult result = objectMapper.readValue(jsonText, EmailClassificationResult.class);
                return Optional.of(result);
            }

        } catch (Exception e) {
            log.error("Failed to call AI API", e);
        }

        return Optional.empty();
    }
}
