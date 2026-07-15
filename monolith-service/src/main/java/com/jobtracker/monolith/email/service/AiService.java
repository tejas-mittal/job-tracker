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
    private static final String PRIMARY_MODEL = "llama-3.3-70b-versatile";
    private static final String[] FALLBACK_MODELS = {
        "llama-3.1-8b-instant",
        "llama-3.2-3b-preview",
        "llama-3.2-1b-preview",
        "mixtral-8x7b-32768" // Last resort just in case
    };

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

    public Optional<EmailClassificationResult> analyzeEmail(String subject, String body, java.time.Instant emailDate) {
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

        String emailContent = "Received Date: " + (emailDate != null ? emailDate.toString() : "") + "\nSubject: " + (subject != null ? subject : "") + "\n\nBody: " + (body != null ? body : "");
        // Truncate email content to ~4000 characters to save tokens and focus on the most important parts
        if (emailContent.length() > 4000) {
            emailContent = emailContent.substring(0, 4000);
        }

        String prompt = """
            You are an expert HR Email Parser. Read the following email and extract the relevant job application details.
            Return a strictly formatted JSON object matching this schema exactly:
            {
              "isJobRelated": boolean, // true ONLY if this email is a DIRECT job application confirmation, interview invite, job rejection, offer, or application withdrawal specifically for the user. FALSE for job recommendations, job alerts, recruiter marketing, newsletters from job boards (like LinkedIn), or promotional emails. CRITICAL: If the email contains phrases like "Thank you for applying", "we can't move forward with your application", or "status of your application", it is absolutely a direct job email and MUST be true. CRITICAL EXCLUSION: If the email is marketing, spam, or about financial products (like mutual funds, NFOs, loans, credit cards, investments), you MUST return false.
              "status": string, // MUST be one of: "APPLIED", "INTERVIEW", "REJECTED", "OFFER", "WITHDRAWN".
              "company": string, // The actual name of the company hiring (e.g. "EPAM India", "Google"). CRITICAL: DO NOT use unofficial or weird sender names like "thandi chaddi" as the company name! Look deep into the email body to find the REAL company name. Return "Unknown Company" if it cannot be found.
              "role": string, // The job title being applied for (e.g., "Software Engineer", "Intern"). Infer from subject or body if possible. If completely unknown, return "Unknown Role".
              "interviewLink": string, // If this is an interview/assessment, extract any Zoom, Google Meet, Teams, HackerRank, or other meeting/test link. (null if none).
              "interviewTime": string, // Extract the FULL date and time of the interview (e.g., "Oct 24, 2026 10:00 AM"). Use the Received Date as context if the email says "tomorrow" or "next Tuesday". (null if none).
              "assessmentDate": string, // Extract the deadline or date for an assessment/test. (null if none).
              "notes": string // Extract extremely useful info: Next steps, required documents, recruiter names, passwords, or Applicant ID. Keep it under 500 chars. (null if none).
            }
            
            Email Content:
            """ + emailContent;

        Optional<EmailClassificationResult> primaryResult = callGroqApi(prompt, PRIMARY_MODEL);
        
        if (primaryResult.isPresent()) {
            return primaryResult;
        }

        log.warn("Primary model {} failed (likely rate limited or exhausted). Iterating through fallback models...", PRIMARY_MODEL);
        
        for (String fallbackModel : FALLBACK_MODELS) {
            Optional<EmailClassificationResult> fallbackResult = callGroqApi(prompt, fallbackModel);
            if (fallbackResult.isPresent()) {
                log.info("Successfully used fallback model {}", fallbackModel);
                return fallbackResult;
            }
            log.warn("Fallback model {} also failed. Trying next...", fallbackModel);
        }
        
        log.error("All AI models completely exhausted or rate-limited for this cycle.");
        return Optional.empty();
    }

    private Optional<EmailClassificationResult> callGroqApi(String prompt, String model) {
        try {
            // Build the Groq (OpenAI-compatible) API request body
            Map<String, Object> requestBody = Map.of(
                    "model", model,
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
                log.error("Groq API error for model {} ({}): {}", model, response.statusCode(), response.body());
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
            log.error("Failed to call AI API using model {}", model, e);
        }

        return Optional.empty();
    }
}
