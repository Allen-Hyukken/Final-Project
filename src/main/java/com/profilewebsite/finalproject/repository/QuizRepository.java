package com.profilewebsite.finalproject.repository;

import com.profilewebsite.finalproject.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByClassRoomId(Long classRoomId);
    List<Quiz> findByTeacherId(Long teacherId);
}
