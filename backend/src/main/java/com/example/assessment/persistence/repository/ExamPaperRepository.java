package com.example.assessment.persistence.repository;

import com.example.assessment.model.ExamPaperType;
import com.example.assessment.persistence.entity.ExamPaperEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamPaperRepository extends JpaRepository<ExamPaperEntity, Long> {
    @EntityGraph(attributePaths = {"teachingAssignment", "teachingAssignment.teacherAccount"})
    Optional<ExamPaperEntity> findFirstByPaperTypeOrderByIdAsc(ExamPaperType paperType);

    Optional<ExamPaperEntity> findByCode(String code);

    @EntityGraph(attributePaths = {"teachingAssignment", "teachingAssignment.teacherAccount"})
    List<ExamPaperEntity> findAllByPaperTypeOrderByStartTimeDescIdDesc(ExamPaperType paperType);

    @EntityGraph(attributePaths = {"teachingAssignment", "teachingAssignment.teacherAccount"})
    List<ExamPaperEntity> findAllByPaperTypeAndTeachingAssignmentTeacherAccountUsernameOrderByStartTimeDescIdDesc(
            ExamPaperType paperType,
            String username
    );

    @EntityGraph(attributePaths = {"teachingAssignment", "teachingAssignment.teacherAccount"})
    List<ExamPaperEntity> findAllByPaperTypeAndTeachingAssignmentIdOrderByStartTimeDescIdDesc(
            ExamPaperType paperType,
            Long teachingAssignmentId
    );

    @EntityGraph(attributePaths = {"teachingAssignment", "teachingAssignment.teacherAccount"})
    Optional<ExamPaperEntity> findWithTeachingAssignmentById(Long id);
}
