package com.profilewebsite.finalproject.repository;

import com.profilewebsite.finalproject.model.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByQuizId(Long quizId);
    List<Attempt> findByStudentId(Long studentId);
    Optional<Attempt> findByQuizIdAndStudentId(Long quizId, Long studentId);
}
