package com.example.assessment.model;

import java.util.List;

public record ProfessionalClassImportResult(
        int createdClassCount,
        int createdStudentCount,
        int updatedStudentCount,
        int skippedCount,
        List<String> messages
) {
}
