package com.example.assessment.dto;

public record StudentUpdateRequest(
        String studentNo,
        String name,
        String className
) {
}
