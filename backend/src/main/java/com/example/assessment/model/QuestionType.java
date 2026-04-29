package com.example.assessment.model;

public enum QuestionType {
    SINGLE_CHOICE("选择题"),
    FILL_BLANK("填空题"),
    SHORT_ANSWER("简答题"),
    DESIGN("设计题"),
    COMPREHENSIVE("综合分析题");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
