package com.example.assessment.model;

public class AnswerRecord {
    private Long questionId;
    private String questionTitle;
    private int maxScore;
    private String studentAnswer;
    private int score;
    private String suggestion;

    public AnswerRecord() {
    }

    public AnswerRecord(Long questionId, String questionTitle, int maxScore, String studentAnswer, int score, String suggestion) {
        this.questionId = questionId;
        this.questionTitle = questionTitle;
        this.maxScore = maxScore;
        this.studentAnswer = studentAnswer;
        this.score = score;
        this.suggestion = suggestion;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
