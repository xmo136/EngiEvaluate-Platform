package com.example.assessment.dto;

import java.util.List;

public record TeachingAssignmentCreateRequest(
        String courseName,
        String className,
        Long teacherAccountId,
        List<Long> professionalClassIds
) {
}
