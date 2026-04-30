package com.example.assessment.dto;

import java.util.List;

public record TeachingAssignmentCreateRequest(
        String courseName,
        String className,
        String courseCode,
        Integer creditHours,
        Integer credits,
        String semester,
        String college,
        String grade,
        Long teacherAccountId,
        List<Long> professionalClassIds
) {
}
