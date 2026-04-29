package com.example.assessment.model;

public class Student {
    private Long id;
    private String studentNo;
    private String name;
    private String className;
    private Long professionalClassId;
    private String courseName;
    private String teacherName;
    private String username;
    private boolean passwordChangeRequired;
    private Long teachingAssignmentId;
    private java.util.List<Long> teachingAssignmentIds;
    private java.util.List<String> teachingAssignmentNames;

    public Student() {
    }

    public Student(Long id, String studentNo, String name, String className) {
        this(id, studentNo, name, className, null, null, null, studentNo, false, null, java.util.List.of(), java.util.List.of());
    }

    public Student(Long id,
                   String studentNo,
                   String name,
                   String className,
                   Long professionalClassId,
                   String courseName,
                   String teacherName,
                   String username,
                   boolean passwordChangeRequired,
                   Long teachingAssignmentId,
                   java.util.List<Long> teachingAssignmentIds,
                   java.util.List<String> teachingAssignmentNames) {
        this.id = id;
        this.studentNo = studentNo;
        this.name = name;
        this.className = className;
        this.professionalClassId = professionalClassId;
        this.courseName = courseName;
        this.teacherName = teacherName;
        this.username = username;
        this.passwordChangeRequired = passwordChangeRequired;
        this.teachingAssignmentId = teachingAssignmentId;
        this.teachingAssignmentIds = teachingAssignmentIds;
        this.teachingAssignmentNames = teachingAssignmentNames;
    }

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

    public Long getProfessionalClassId() {
        return professionalClassId;
    }

    public void setProfessionalClassId(Long professionalClassId) {
        this.professionalClassId = professionalClassId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public Long getTeachingAssignmentId() {
        return teachingAssignmentId;
    }

    public void setTeachingAssignmentId(Long teachingAssignmentId) {
        this.teachingAssignmentId = teachingAssignmentId;
    }

    public java.util.List<Long> getTeachingAssignmentIds() {
        return teachingAssignmentIds;
    }

    public void setTeachingAssignmentIds(java.util.List<Long> teachingAssignmentIds) {
        this.teachingAssignmentIds = teachingAssignmentIds;
    }

    public java.util.List<String> getTeachingAssignmentNames() {
        return teachingAssignmentNames;
    }

    public void setTeachingAssignmentNames(java.util.List<String> teachingAssignmentNames) {
        this.teachingAssignmentNames = teachingAssignmentNames;
    }
}
