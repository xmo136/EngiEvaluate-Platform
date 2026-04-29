package com.example.assessment.dto;

public record TeacherAccountCreateRequest(
        String username,
        String displayName,
        String password
) {
}
