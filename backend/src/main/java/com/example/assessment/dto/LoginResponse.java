package com.example.assessment.dto;

import com.example.assessment.model.UserRole;

import java.util.List;

public record LoginResponse(
        Long id,
        String username,
        String displayName,
        UserRole role,
        String roleLabel,
        Long studentId,
        List<String> allowedViews,
        boolean mustChangePassword
) {
}
