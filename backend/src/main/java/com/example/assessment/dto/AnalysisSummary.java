package com.example.assessment.dto;

import java.util.List;
import java.util.Map;

public record AnalysisSummary(
        int studentCount,
        int questionCount,
        double averageScore,
        Map<String, Long> scoreBands,
        Map<String, Long> questionTypeCount,
        Map<String, Double> objectiveAverage,
        List<String> improvementSuggestions
) {
}
