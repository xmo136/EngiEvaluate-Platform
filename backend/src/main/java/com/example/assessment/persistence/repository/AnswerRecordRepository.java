package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.AnswerRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecordEntity, Long> {
    @Query("select count(ar.id) from AnswerRecordEntity ar where ar.question.id = :questionId")
    long countByQuestionId(@Param("questionId") Long questionId);
}
