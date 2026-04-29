package com.example.assessment.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExamResult {
    private Long id;
    private Long examId;
    private Long teachingAssignmentId;
    private Student student;
    private String courseName;
    private String paperName;
    private String className;
    private int totalScore;
    private LocalDateTime submittedAt;
    private List<AnswerRecord> answers;
    private Map<CourseObjective, Integer> objectiveScores;

    public ExamResult() {
    }

    public ExamResult(Long id, Long examId, Long teachingAssignmentId, Student student, String courseName, String paperName,
                      String className, int totalScore, LocalDateTime submittedAt, List<AnswerRecord> answers,
                      Map<CourseObjective, Integer> objectiveScores) {
        this.id = id;
        this.examId = examId;
        this.teachingAssignmentId = teachingAssignmentId;
        this.student = student;
        this.courseName = courseName;
        this.paperName = paperName;
        this.className = className;
        this.totalScore = totalScore;
        this.submittedAt = submittedAt;
        this.answers = answers;
        this.objectiveScores = objectiveScores;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getTeachingAssignmentId() {
        return teachingAssignmentId;
    }

    public void setTeachingAssignmentId(Long teachingAssignmentId) {
        this.teachingAssignmentId = teachingAssignmentId;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getPaperName() {
        return paperName;
    }

    public void setPaperName(String paperName) {
        this.paperName = paperName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<AnswerRecord> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerRecord> answers) {
        this.answers = answers;
    }

    public Map<CourseObjective, Integer> getObjectiveScores() {
        return objectiveScores;
    }

    public void setObjectiveScores(Map<CourseObjective, Integer> objectiveScores) {
        this.objectiveScores = objectiveScores;
    }
}
