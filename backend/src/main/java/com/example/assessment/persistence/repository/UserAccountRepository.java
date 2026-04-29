package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {
    @EntityGraph(attributePaths = "student")
    Optional<UserAccountEntity> findByUsername(String username);

    List<UserAccountEntity> findAllByRoleOrderByIdAsc(com.example.assessment.model.UserRole role);

    @EntityGraph(attributePaths = "student")
    List<UserAccountEntity> findAllByStudentIdIn(Collection<Long> studentIds);

    Optional<UserAccountEntity> findByStudentId(Long studentId);
}
