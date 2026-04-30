package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.RegularGradeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegularGradeRepository extends JpaRepository<RegularGradeEntity, Long> {
    @EntityGraph(attributePaths = {"student", "teachingAssignment"})
    List<RegularGradeEntity> findAllByTeachingAssignmentId(Long teachingAssignmentId);

    Optional<RegularGradeEntity> findByStudentIdAndTeachingAssignmentId(Long studentId, Long teachingAssignmentId);
}
