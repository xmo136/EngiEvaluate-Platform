package com.example.assessment.model;

import java.util.List;

public class MockExamResultGenerationResult {
    private Long examId;
    private int createdCount;
    private int skippedCount;
    private List<String> messages;

    public MockExamResultGenerationResult() {
    }

    public MockExamResultGenerationResult(Long examId, int createdCount, int skippedCount, List<String> messages) {
        this.examId = examId;
        this.createdCount = createdCount;
        this.skippedCount = skippedCount;
        this.messages = messages;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
