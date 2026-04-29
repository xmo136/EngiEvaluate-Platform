package com.example.assessment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ExamPaperSaveRequest(Long teachingAssignmentId,
                                   String paperName,
                                   String description,
                                   LocalDateTime startTime,
                                   Integer durationMinutes,
                                   List<Long> questionIds) {
}
