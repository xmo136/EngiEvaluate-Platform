package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.TeachingAssignmentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignmentEntity, Long> {
    @EntityGraph(attributePaths = {"teacherAccount", "professionalClasses"})
    List<TeachingAssignmentEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"teacherAccount", "professionalClasses"})
    List<TeachingAssignmentEntity> findAllByTeacherAccountUsernameOrderByIdAsc(String username);

    long countByTeacherAccountId(Long teacherAccountId);

    @EntityGraph(attributePaths = {"teacherAccount", "professionalClasses"})
    Optional<TeachingAssignmentEntity> findById(Long id);
}
