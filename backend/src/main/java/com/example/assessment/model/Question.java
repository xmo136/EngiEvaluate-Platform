package com.example.assessment.model;

import java.util.List;

public class Question {
    private Long id;
    private String courseName;
    private String title;
    private QuestionType type;
    private CourseObjective objective;
    private int score;
    private List<String> options;
    private String answer;
    private String analysis;

    public Question() {
    }

    public Question(Long id, String courseName, String title, QuestionType type, CourseObjective objective,
                    int score, List<String> options, String answer, String analysis) {
        this.id = id;
        this.courseName = courseName;
        this.title = title;
        this.type = type;
        this.objective = objective;
        this.score = score;
        this.options = options;
        this.answer = answer;
        this.analysis = analysis;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public CourseObjective getObjective() {
        return objective;
    }

    public void setObjective(CourseObjective objective) {
        this.objective = objective;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
