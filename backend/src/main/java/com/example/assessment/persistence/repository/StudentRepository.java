package com.example.assessment.persistence.repository;

import com.example.assessment.persistence.entity.StudentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    @EntityGraph(attributePaths = {"teachingAssignments.teacherAccount", "professionalClass"})
    @Query("select distinct s from StudentEntity s order by s.id asc")
    List<StudentEntity> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"teachingAssignments.teacherAccount", "professionalClass"})
    @Query("""
            select distinct s
            from StudentEntity s
            join s.teachingAssignments ta
            where ta.teacherAccount.username = :username
            order by s.id asc
            """)
    List<StudentEntity> findAllByTeachingAssignmentsTeacherAccountUsernameOrderByIdAsc(@Param("username") String username);

    @EntityGraph(attributePaths = {"teachingAssignments.teacherAccount", "professionalClass"})
    @Query("""
            select distinct s
            from StudentEntity s
            join s.teachingAssignments ta
            where ta.id = :teachingAssignmentId
            order by s.id asc
            """)
    List<StudentEntity> findAllByTeachingAssignmentsIdOrderByIdAsc(@Param("teachingAssignmentId") Long teachingAssignmentId);

    @EntityGraph(attributePaths = {"teachingAssignments.teacherAccount", "professionalClass"})
    @Query("""
            select distinct s
            from StudentEntity s
            where s.professionalClass.id = :professionalClassId
            order by s.id asc
            """)
    List<StudentEntity> findAllByProfessionalClassIdOrderByIdAsc(@Param("professionalClassId") Long professionalClassId);

    @EntityGraph(attributePaths = {"teachingAssignments.teacherAccount", "professionalClass"})
    @Query("""
            select distinct s
            from StudentEntity s
            left join s.teachingAssignments ta
            where s.id = :studentId
            """)
    Optional<StudentEntity> findDetailedById(@Param("studentId") Long studentId);

    Optional<StudentEntity> findByStudentNo(String studentNo);

    long countByTeachingAssignmentsId(Long teachingAssignmentId);

    long countByProfessionalClassId(Long professionalClassId);
}
