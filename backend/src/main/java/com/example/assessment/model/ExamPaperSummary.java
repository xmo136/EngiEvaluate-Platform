package com.example.assessment.model;

import java.time.LocalDateTime;

public class ExamPaperSummary {
    private Long id;
    private Long teachingAssignmentId;
    private String courseName;
    private String className;
    private String paperName;
    private String description;
    private LocalDateTime startTime;
    private int durationMinutes;
    private int questionCount;
    private int totalScore;
    private boolean active;
    private int submittedCount;
    private boolean submitted;

    public ExamPaperSummary() {
    }

    public ExamPaperSummary(Long id, Long teachingAssignmentId, String courseName, String className, String paperName,
                            String description, LocalDateTime startTime, int durationMinutes, int questionCount,
                            int totalScore, boolean active, int submittedCount, boolean submitted) {
        this.id = id;
        this.teachingAssignmentId = teachingAssignmentId;
        this.courseName = courseName;
        this.className = className;
        this.paperName = paperName;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.questionCount = questionCount;
        this.totalScore = totalScore;
        this.active = active;
        this.submittedCount = submittedCount;
        this.submitted = submitted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeachingAssignmentId() {
        return teachingAssignmentId;
    }

    public void setTeachingAssignmentId(Long teachingAssignmentId) {
        this.teachingAssignmentId = teachingAssignmentId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(int submittedCount) {
        this.submittedCount = submittedCount;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }
}
