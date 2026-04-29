package com.example.assessment.dto;

public record StudentCreateRequest(
        String studentNo,
        String name,
        String className
) {
}
