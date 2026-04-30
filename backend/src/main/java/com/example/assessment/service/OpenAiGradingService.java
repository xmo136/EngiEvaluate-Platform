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
import java.util.ArrayList;
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

            StringBuilder suggestion = new StringBuilder("AI评阅：");
            suggestion.append(rationale.isBlank() ? "模型已根据参考答案与评分要点完成评分" : rationale);
            if (question.getType() == QuestionType.FILL_BLANK) {
                suggestion.append(semanticMatch ? "；语义判定通过" : "；语义判定未通过");
            }
            if (!matchedPoints.isBlank()) {
                suggestion.append("；命中要点：").append(matchedPoints);
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
                    你是一名严谨的中文考试阅卷助手。
                    请判断填空题答案是否与参考答案语义等价，可以接受近义表达、同义术语和常见技术表述变体。
                    只返回 JSON 对象，不要输出额外说明。
                    填空题只能给 0 分或满分。
                    rationale 字段必须使用中文。
                    JSON 结构如下：
                    {"awardedScore": integer, "semanticMatch": boolean, "rationale": string, "matchedPoints": string[]}
                    """;
        }
        return """
                你是一名严谨的中文考试阅卷助手。
                请根据参考答案和评分要点为学生答案评分，优先考虑关键点覆盖、内容准确性和与题目要求的相关性。
                只返回 JSON 对象，不要输出额外说明。
                rationale 字段必须使用中文。
                JSON 结构如下：
                {"awardedScore": integer, "semanticMatch": boolean, "rationale": string, "matchedPoints": string[]}
                """;
    }

    private String buildUserPrompt(QuestionEntity question, String studentAnswer) {
        return """
                请基于以下信息完成评分：

                题型：%s
                题目：%s
                满分：%s
                课程目标：%s
                参考答案：%s
                评分要点：%s
                学生答案：%s
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

    public List<String> generateImprovementSuggestions(Map<String, Double> objectiveAverage, double average) {
        Map<String, String> perObjective = generatePerObjectiveSuggestions(objectiveAverage, average);
        return new ArrayList<>(perObjective.values());
    }

    public Map<String, String> generatePerObjectiveSuggestions(Map<String, Double> objectiveAverage, double average) {
        if (!enabled || apiKey.isBlank()) {
            return fallbackPerObjectiveSuggestions(objectiveAverage, average);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildPerObjectiveRequestBody(objectiveAverage, average))))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("AI suggestion request failed with status {} and body {}", response.statusCode(), response.body());
                return fallbackPerObjectiveSuggestions(objectiveAverage, average);
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                log.warn("AI suggestion response did not contain message content: {}", response.body());
                return fallbackPerObjectiveSuggestions(objectiveAverage, average);
            }

            String jsonStr = extractJson(content);
            JsonNode resultNode = objectMapper.readTree(jsonStr);
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : objectiveAverage.entrySet()) {
                String key = entry.getKey();
                JsonNode node = resultNode.path(key);
                if (node.isTextual() && !node.asText("").isBlank()) {
                    result.put(key, node.asText(""));
                } else {
                    return fallbackPerObjectiveSuggestions(objectiveAverage, average);
                }
            }
            return result.isEmpty() ? fallbackPerObjectiveSuggestions(objectiveAverage, average) : result;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("AI suggestion request failed", ex);
            return fallbackPerObjectiveSuggestions(objectiveAverage, average);
        }
    }

    private Map<String, Object> buildPerObjectiveRequestBody(Map<String, Double> objectiveAverage, double average) {
        StringBuilder dataDesc = new StringBuilder();
        dataDesc.append("总体平均分：").append(String.format("%.1f", average)).append("\n");
        dataDesc.append("各课程目标达成度：\n");
        for (Map.Entry<String, Double> entry : objectiveAverage.entrySet()) {
            dataDesc.append("  - ").append(entry.getKey()).append("：").append(String.format("%.3f", entry.getValue())).append("\n");
        }

        String systemPrompt = """
                你是一名专业的工程教育评估分析助手。
                请根据提供的考试数据，为每个课程目标分别生成"评价结果分析"和"持续改进"建议。
                要求：
                1. 使用中文
                2. 每个课程目标的文本格式为：评价结果分析：xxx。持续改进：xxx。
                3. 评价结果分析应结合该目标的达成度数据，分析达成情况（良好/中等/基本达成/需改进）
                4. 持续改进建议应具体、可操作，针对该目标的薄弱环节
                返回 JSON 对象，key 为课程目标名称，value 为完整的"评价结果分析：... 持续改进：..."文本。
                """;

        StringBuilder jsonFormat = new StringBuilder("{\n");
        List<String> keys = new ArrayList<>(objectiveAverage.keySet());
        for (int i = 0; i < keys.size(); i++) {
            jsonFormat.append("  \"").append(keys.get(i)).append("\": \"评价结果分析：... 持续改进：...\"");
            if (i < keys.size() - 1) jsonFormat.append(",");
            jsonFormat.append("\n");
        }
        jsonFormat.append("}");

        String userPrompt = "请根据以下考试分析数据，为每个课程目标生成评价结果分析和持续改进建议：\n\n"
                + dataDesc + "\n返回格式示例：\n" + jsonFormat;

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", model);
        request.put("stream", false);
        request.put("max_tokens", 1024);
        request.put("temperature", 0.7);
        request.put("top_p", topP);
        request.put("frequency_penalty", frequencyPenalty);
        request.put("extra_body", Map.of("top_k", topK));
        request.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        return request;
    }

    private Map<String, String> fallbackPerObjectiveSuggestions(Map<String, Double> objectiveAverage, double average) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : objectiveAverage.entrySet()) {
            String name = entry.getKey();
            double value = entry.getValue();
            String level = value >= 0.8 ? "良好" : value >= 0.7 ? "中等" : value >= 0.6 ? "基本达成" : "需改进";
            String analysis = name + "当前达成评价值为" + String.format("%.3f", value) + "，达成情况" + level + "。";
            String suggestion;
            if (value < 0.7) {
                suggestion = name + "达成度偏低，建议增加针对性练习和反馈，强化薄弱知识点的教学。";
            } else if (value >= 0.8) {
                suggestion = name + "达成情况良好，建议继续保持当前教学方法，关注低分学生的个别辅导。";
            } else {
                suggestion = name + "达成情况中等，建议适当增加相关练习题和案例分析，提升学生综合应用能力。";
            }
            result.put(name, "评价结果分析：" + analysis + "持续改进：" + suggestion);
        }
        return result;
    }

    public record AiGradingDecision(int score, String suggestion) {
    }
}
