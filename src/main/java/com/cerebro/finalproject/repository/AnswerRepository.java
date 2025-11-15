package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findByAttemptId(Long attemptId);

    List<Answer> findByQuestionId(Long questionId);

    void deleteByAttemptId(Long attemptId);
}