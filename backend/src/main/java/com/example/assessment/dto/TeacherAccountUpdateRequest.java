package com.example.assessment.dto;

public record TeacherAccountUpdateRequest(
        String username,
        String displayName,
        String password
) {
}
