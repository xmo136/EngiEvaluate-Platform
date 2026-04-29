package com.example.assessment.dto;

import java.util.List;

public record TeachingAssignmentUpdateRequest(
        String courseName,
        String className,
        Long teacherAccountId,
        List<Long> professionalClassIds
) {
}
