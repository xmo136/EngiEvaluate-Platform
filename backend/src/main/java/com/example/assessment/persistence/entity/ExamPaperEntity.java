package com.example.assessment.persistence.entity;

import com.example.assessment.model.ExamPaperType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_papers")
public class ExamPaperEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(name = "course_name", nullable = false, length = 128)
    private String courseName;

    @Column(name = "paper_name", nullable = false, length = 128)
    private String paperName;

    @Column(length = 512)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_type", nullable = false, length = 32)
    private ExamPaperType paperType = ExamPaperType.EXAM;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teaching_assignment_id")
    private TeachingAssignmentEntity teachingAssignment;

    @Column(nullable = false)
    private boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExamPaperType getPaperType() {
        return paperType;
    }

    public void setPaperType(ExamPaperType paperType) {
        this.paperType = paperType;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public TeachingAssignmentEntity getTeachingAssignment() {
        return teachingAssignment;
    }

    public void setTeachingAssignment(TeachingAssignmentEntity teachingAssignment) {
        this.teachingAssignment = teachingAssignment;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
