package com.example.assessment.model;

public class UserAccount {
    private Long id;
    private String username;
    private String password;
    private String displayName;
    private UserRole role;
    private Long studentId;

    public UserAccount() {
    }

    public UserAccount(Long id, String username, String password, String displayName, UserRole role, Long studentId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.studentId = studentId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
