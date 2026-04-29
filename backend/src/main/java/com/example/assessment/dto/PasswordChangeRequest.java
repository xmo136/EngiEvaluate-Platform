package com.example.assessment.dto;

public record PasswordChangeRequest(
        String currentPassword,
        String newPassword
) {
}
