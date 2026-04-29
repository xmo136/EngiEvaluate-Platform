package com.example.assessment.model;

import java.util.List;

public record QuestionImportResult(
        int createdCount,
        int skippedCount,
        List<String> messages
) {
}
