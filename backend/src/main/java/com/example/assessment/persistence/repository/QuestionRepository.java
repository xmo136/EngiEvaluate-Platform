package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.QuestionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    @EntityGraph(attributePaths = {"paper", "options"})
    List<QuestionEntity> findAllByPaperIdOrderBySortOrderAscIdAsc(Long paperId);

    @EntityGraph(attributePaths = {"paper", "options"})
    Optional<QuestionEntity> findById(Long id);

    @EntityGraph(attributePaths = {"paper", "options"})
    List<QuestionEntity> findAllByIdIn(List<Long> ids);

    @Query("select coalesce(max(q.sortOrder), 0) from QuestionEntity q where q.paper.id = :paperId")
    int findMaxSortOrderByPaperId(@Param("paperId") Long paperId);

    long countByPaperId(Long paperId);
}
