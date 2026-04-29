package com.example.assessment.model;

public enum CourseObjective {
    OBJECTIVE_1("课程目标1"),
    OBJECTIVE_2("课程目标2"),
    OBJECTIVE_3("课程目标3"),
    OBJECTIVE_4("课程目标4");

    private final String label;

    CourseObjective(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
