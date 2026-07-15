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
 * Service for communicating with the Google Gemini API to classify and extract data from emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

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
            log.warn("GEMINI_API_KEY is missing!");
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
              "isJobRelated": boolean, // true ONLY if this email is a job application, interview invite, job rejection, offer, or application withdrawal. This includes ANY automated email saying "Application has been submitted successfully", "Thanks for applying", "Your applications were sent", "Applied on", "Indeed Application:", "Creating Your Application Form", or "Application Confirmation". False for marketing, newsletters, or unrelated emails.
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
            // Build the Gemini API request body
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json"
                    )
            );

            String requestBodyJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + this.apiKey.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error ({}): {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            // Parse the Gemini response
            var rootNode = objectMapper.readTree(response.body());
            var candidates = rootNode.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                String jsonText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                
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
            log.error("Failed to call Gemini API", e);
        }

        return Optional.empty();
    }
}
