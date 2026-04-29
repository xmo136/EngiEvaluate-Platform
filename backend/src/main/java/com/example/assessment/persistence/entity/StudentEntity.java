package com.example.assessment.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "students")
public class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_no", nullable = false, unique = true, length = 32)
    private String studentNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    @ManyToOne
    @JoinColumn(name = "professional_class_id")
    private ProfessionalClassEntity professionalClass;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teaching_assignment_students",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "teaching_assignment_id")
    )
    private Set<TeachingAssignmentEntity> teachingAssignments = new LinkedHashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ProfessionalClassEntity getProfessionalClass() {
        return professionalClass;
    }

    public void setProfessionalClass(ProfessionalClassEntity professionalClass) {
        this.professionalClass = professionalClass;
    }

    public Set<TeachingAssignmentEntity> getTeachingAssignments() {
        return teachingAssignments;
    }

    public void setTeachingAssignments(Set<TeachingAssignmentEntity> teachingAssignments) {
        this.teachingAssignments = teachingAssignments;
    }
}
