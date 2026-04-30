package com.example.assessment.dto;

import java.util.List;

public record RegularGradeSaveRequest(
        Long teachingAssignmentId,
        List<RegularGradeEntry> grades
) {
    public record RegularGradeEntry(
            Long studentId,
            Integer labScore,
            Integer homeworkScore,
            Integer classScore
    ) {}
}
