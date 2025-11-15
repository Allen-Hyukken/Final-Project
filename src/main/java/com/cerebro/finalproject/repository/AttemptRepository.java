package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    List<Attempt> findByQuizId(Long quizId);

    List<Attempt> findByStudentId(Long studentId);

    List<Attempt> findByQuizIdAndStudentId(Long quizId, Long studentId);

    Optional<Attempt> findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(Long quizId, Long studentId);

    boolean existsByQuizIdAndStudentId(Long quizId, Long studentId);
}