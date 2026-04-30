package com.example.assessment.model;

import java.util.List;

public record TeachingAssignment(
        Long id,
        String courseName,
        String className,
        String courseCode,
        Integer creditHours,
        Integer credits,
        String semester,
        String college,
        String grade,
        Long teacherAccountId,
        String teacherUsername,
        String teacherName,
        List<Long> professionalClassIds,
        List<String> professionalClassNames
) {
}
