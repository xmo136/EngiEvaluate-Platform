package com.example.assessment.model;

public enum UserRole {
    ADMIN("管理员"),
    TEACHER("教师"),
    STUDENT("学生");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
