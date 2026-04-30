package com.example.assessment.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "teaching_assignments")
public class TeachingAssignmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_name", nullable = false, length = 128)
    private String courseName;

    @Column(name = "class_name", nullable = false, length = 64)
    private String className;

    @Column(name = "course_code", length = 32)
    private String courseCode;

    @Column(name = "credit_hours")
    private Integer creditHours;

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "semester", length = 32)
    private String semester;

    @Column(name = "college", length = 64)
    private String college;

    @Column(name = "grade", length = 16)
    private String grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_account_id")
    private UserAccountEntity teacherAccount;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teaching_assignment_professional_classes",
            joinColumns = @JoinColumn(name = "teaching_assignment_id"),
            inverseJoinColumns = @JoinColumn(name = "professional_class_id")
    )
    private Set<ProfessionalClassEntity> professionalClasses = new LinkedHashSet<>();

    @ManyToMany(mappedBy = "teachingAssignments", fetch = FetchType.LAZY)
    private Set<StudentEntity> students = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "teaching_assignment_excluded_students",
            joinColumns = @JoinColumn(name = "teaching_assignment_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<StudentEntity> excludedStudents = new LinkedHashSet<>();

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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Integer getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(Integer creditHours) {
        this.creditHours = creditHours;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public UserAccountEntity getTeacherAccount() {
        return teacherAccount;
    }

    public void setTeacherAccount(UserAccountEntity teacherAccount) {
        this.teacherAccount = teacherAccount;
    }

    public Set<ProfessionalClassEntity> getProfessionalClasses() {
        return professionalClasses;
    }

    public void setProfessionalClasses(Set<ProfessionalClassEntity> professionalClasses) {
        this.professionalClasses = professionalClasses;
    }

    public Set<StudentEntity> getStudents() {
        return students;
    }

    public void setStudents(Set<StudentEntity> students) {
        this.students = students;
    }

    public Set<StudentEntity> getExcludedStudents() {
        return excludedStudents;
    }

    public void setExcludedStudents(Set<StudentEntity> excludedStudents) {
        this.excludedStudents = excludedStudents;
    }
}
