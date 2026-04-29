package com.example.assessment.model;

import java.util.List;

public record StudentImportResult(
        int createdCount,
        int skippedCount,
        List<String> messages
) {
}
