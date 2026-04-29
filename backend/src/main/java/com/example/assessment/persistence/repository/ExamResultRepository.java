package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.ExamResultEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResultEntity, Long> {
    @EntityGraph(attributePaths = {"student", "paper", "answers", "answers.question"})
    List<ExamResultEntity> findAllByOrderBySubmittedAtDesc();

    @EntityGraph(attributePaths = {"student", "paper", "answers", "answers.question"})
    Optional<ExamResultEntity> findById(Long id);

    @EntityGraph(attributePaths = {"student", "paper", "paper.teachingAssignment", "answers", "answers.question"})
    List<ExamResultEntity> findAllByPaperTeachingAssignmentTeacherAccountUsernameOrderBySubmittedAtDesc(String username);

    @EntityGraph(attributePaths = {"student", "paper", "paper.teachingAssignment", "answers", "answers.question"})
    List<ExamResultEntity> findAllByPaperIdOrderBySubmittedAtDesc(Long paperId);

    long countByPaperId(Long paperId);

    long countByPaperIdAndStudentId(Long paperId, Long studentId);

    long countByStudentId(Long studentId);

    void deleteByStudentId(Long studentId);
}
