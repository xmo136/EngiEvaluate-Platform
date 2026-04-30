package com.example.assessment.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

@Entity
@Table(name = "regular_grades", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "teaching_assignment_id"}))
public class RegularGradeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentEntity student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_assignment_id", nullable = false)
    private TeachingAssignmentEntity teachingAssignment;

    @Column(name = "lab_score")
    private Integer labScore;

    @Column(name = "homework_score")
    private Integer homeworkScore;

    @Column(name = "class_score")
    private Integer classScore;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StudentEntity getStudent() { return student; }
    public void setStudent(StudentEntity student) { this.student = student; }

    public TeachingAssignmentEntity getTeachingAssignment() { return teachingAssignment; }
    public void setTeachingAssignment(TeachingAssignmentEntity teachingAssignment) { this.teachingAssignment = teachingAssignment; }

    public Integer getLabScore() { return labScore; }
    public void setLabScore(Integer labScore) { this.labScore = labScore; }

    public Integer getHomeworkScore() { return homeworkScore; }
    public void setHomeworkScore(Integer homeworkScore) { this.homeworkScore = homeworkScore; }

    public Integer getClassScore() { return classScore; }
    public void setClassScore(Integer classScore) { this.classScore = classScore; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
