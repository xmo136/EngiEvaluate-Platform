package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.ProfessionalClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProfessionalClassRepository extends JpaRepository<ProfessionalClassEntity, Long> {
    List<ProfessionalClassEntity> findAllByOrderByNameAsc();

    Optional<ProfessionalClassEntity> findByName(String name);

    List<ProfessionalClassEntity> findAllByIdIn(Collection<Long> ids);
}
