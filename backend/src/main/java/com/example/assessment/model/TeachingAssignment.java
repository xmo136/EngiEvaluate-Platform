package com.example.assessment.model;

import java.util.List;

public record TeachingAssignment(
        Long id,
        String courseName,
        String className,
        Long teacherAccountId,
        String teacherUsername,
        String teacherName,
        List<Long> professionalClassIds,
        List<String> professionalClassNames
) {
}
