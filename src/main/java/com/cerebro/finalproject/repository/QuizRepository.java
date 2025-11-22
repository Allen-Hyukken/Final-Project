package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByClassRoomId(Long classRoomId);

    List<Quiz> findByTeacherId(Long teacherId);

    List<Quiz> findByClassRoomIdAndPublished(Long classRoomId, Boolean published);

    @Modifying
    @Transactional
    @Query(value = "UPDATE quiz q SET q.total_points = " +
            "(SELECT COALESCE(SUM(points), 0) FROM question WHERE quiz_id = q.id) " +
            "WHERE q.id = :quizId", nativeQuery = true)
    void updateTotalPoints(@Param("quizId") Long quizId);
}