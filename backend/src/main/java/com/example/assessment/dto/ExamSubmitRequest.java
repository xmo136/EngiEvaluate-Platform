package com.example.assessment.dto;

import java.util.Map;

public record ExamSubmitRequest(Long examId, Long studentId, Map<Long, String> answers) {
}
