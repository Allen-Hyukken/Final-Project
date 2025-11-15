package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    @Query("SELECT q FROM Question q WHERE q.quiz.id = :quizId ORDER BY q.qIndex ASC")
    List<Question> findByQuizIdOrderByQIndexAsc(@Param("quizId") Long quizId);

    List<Question> findByQuizId(Long quizId);
}