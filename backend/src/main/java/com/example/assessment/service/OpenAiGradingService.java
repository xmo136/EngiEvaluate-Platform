package com.example.assessment.service;

import com.example.assessment.model.QuestionType;
import com.example.assessment.persistence.entity.QuestionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OpenAiGradingService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiGradingService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final double temperature;
    private final double topP;
    private final int topK;
    private final double frequencyPenalty;

    public OpenAiGradingService(ObjectMapper objectMapper,
                                @Value("${assessment.ai-grading.enabled:false}") boolean enabled,
                                @Value("${assessment.ai-grading.api-key:}") String apiKey,
                                @Value("${assessment.ai-grading.base-url:https://ai.gitee.com/v1}") String baseUrl,
                                @Value("${assessment.ai-grading.model:DeepSeek-V4-Flash}") String model,
                                @Value("${assessment.ai-grading.timeout-seconds:45}") int timeoutSeconds,
                                @Value("${assessment.ai-grading.max-tokens:1024}") int maxTokens,
                                @Value("${assessment.ai-grading.temperature:0.7}") double temperature,
                                @Value("${assessment.ai-grading.top-p:0.7}") double topP,
                                @Value("${assessment.ai-grading.top-k:50}") int topK,
                                @Value("${assessment.ai-grading.frequency-penalty:1}") double frequencyPenalty) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = baseUrl == null ? "https://ai.gitee.com/v1" : baseUrl.trim();
        this.model = model == null || model.isBlank() ? "DeepSeek-V4-Flash" : model.trim();
        this.timeoutSeconds = Math.max(10, timeoutSeconds);
        this.maxTokens = Math.max(256, maxTokens);
        this.temperature = temperature;
        this.topP = topP;
        this.topK = Math.max(1, topK);
        this.frequencyPenalty = frequencyPenalty;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(this.timeoutSeconds, 20)))
                .build();
    }

    public Optional<AiGradingDecision> gradeQuestion(QuestionEntity question, String studentAnswer) {
        if (!enabled || apiKey.isBlank() || question == null || question.getType() == QuestionType.SINGLE_CHOICE) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildRequestBody(question, studentAnswer))))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI grading request failed with status {} and body {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String content = contentNode.isTextual() ? contentNode.asText("") : "";
            if (content.isBlank()) {
                log.warn("AI grading response did not contain message content: {}", response.body());
                return Optional.empty();
            }

            JsonNode gradingJson = objectMapper.readTree(extractJson(content));
            int score = gradingJson.path("awardedScore").asInt();
            String rationale = gradingJson.path("rationale").asText("");
            boolean semanticMatch = gradingJson.path("semanticMatch").asBoolean(false);
            String matchedPoints = "";
            if (gradingJson.path("matchedPoints").isArray()) {
                List<String> matchedPointList = objectMapper.convertValue(
                        gradingJson.path("matchedPoints"),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                );
                matchedPoints = String.join("; ", matchedPointList);
            }

            StringBuilder suggestion = new StringBuilder("AI grading note: ");
            suggestion.append(rationale.isBlank() ? "Scored by the configured model" : rationale);
            if (question.getType() == QuestionType.FILL_BLANK) {
                suggestion.append(semanticMatch ? "; semantic match accepted" : "; semantic match not accepted");
            }
            if (!matchedPoints.isBlank()) {
                suggestion.append("; matched points: ").append(matchedPoints);
            }
            return Optional.of(new AiGradingDecision(score, suggestion.toString()));
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("AI grading request failed", ex);
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequestBody(QuestionEntity question, String studentAnswer) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("stream", false);
        request.put("max_tokens", maxTokens);
        request.put("temperature", temperature);
        request.put("top_p", topP);
        request.put("frequency_penalty", frequencyPenalty);
        request.put("extra_body", Map.of("top_k", topK));
        request.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt(question)),
                Map.of("role", "user", "content", buildUserPrompt(question, studentAnswer))
        ));
        return request;
    }

    private String buildSystemPrompt(QuestionEntity question) {
        if (question.getType() == QuestionType.FILL_BLANK) {
            return """
                    You are a rigorous exam grader.
                    Evaluate whether a fill-in-the-blank answer is semantically equivalent to the reference answer.
                    Allow synonyms, paraphrases, and equivalent technical terms.
                    Return only a JSON object.
                    For fill-in-the-blank questions, award either 0 or full credit only.
                    The JSON schema is:
                    {"awardedScore": integer, "semanticMatch": boolean, "rationale": string, "matchedPoints": string[]}
                    """;
        }
        return """
                You are a rigorous exam grader.
                Score the student's answer against the reference answer and scoring guidance.
                Prioritize coverage of key points, factual accuracy, and relevance to the question.
                Return only a JSON object.
                The JSON schema is:
                {"awardedScore": integer, "semanticMatch": boolean, "rationale": string, "matchedPoints": string[]}
                """;
    }

    private String buildUserPrompt(QuestionEntity question, String studentAnswer) {
        return """
                Grade the answer using the following information:

                Question type: %s
                Question: %s
                Max score: %s
                Objective: %s
                Reference answer: %s
                Scoring guidance: %s
                Student answer: %s
                """.formatted(
                question.getType(),
                safe(question.getTitle()),
                question.getScore(),
                question.getObjective(),
                safe(question.getAnswer()),
                safe(question.getAnalysis()),
                safe(studentAnswer)
        );
    }

    private String extractJson(String content) {
        String normalized = content.trim();
        if (normalized.startsWith("```")) {
            int firstBrace = normalized.indexOf('{');
            int lastBrace = normalized.lastIndexOf('}');
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return normalized.substring(firstBrace, lastBrace + 1);
            }
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value.trim();
    }

    public record AiGradingDecision(int score, String suggestion) {
    }
}
