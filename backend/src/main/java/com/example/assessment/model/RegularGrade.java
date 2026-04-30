package com.example.assessment.model;

import java.time.LocalDateTime;

public class RegularGrade {
    private Long id;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private Long teachingAssignmentId;
    private Integer labScore;
    private Integer homeworkScore;
    private Integer classScore;
    private Integer examScore;
    private Double totalScore;
    private LocalDateTime updatedAt;

    public RegularGrade() {}

    public RegularGrade(Long id, Long studentId, String studentNo, String studentName,
                        Long teachingAssignmentId, Integer labScore, Integer homeworkScore,
                        Integer classScore, Integer examScore, Double totalScore, LocalDateTime updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.studentNo = studentNo;
        this.studentName = studentName;
        this.teachingAssignmentId = teachingAssignmentId;
        this.labScore = labScore;
        this.homeworkScore = homeworkScore;
        this.classScore = classScore;
        this.examScore = examScore;
        this.totalScore = totalScore;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Long getTeachingAssignmentId() { return teachingAssignmentId; }
    public void setTeachingAssignmentId(Long teachingAssignmentId) { this.teachingAssignmentId = teachingAssignmentId; }

    public Integer getLabScore() { return labScore; }
    public void setLabScore(Integer labScore) { this.labScore = labScore; }

    public Integer getHomeworkScore() { return homeworkScore; }
    public void setHomeworkScore(Integer homeworkScore) { this.homeworkScore = homeworkScore; }

    public Integer getClassScore() { return classScore; }
    public void setClassScore(Integer classScore) { this.classScore = classScore; }

    public Integer getExamScore() { return examScore; }
    public void setExamScore(Integer examScore) { this.examScore = examScore; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
