package com.cerebro.finalproject.repository;

import com.cerebro.finalproject.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByClassRoomId(Long classRoomId);

    List<Quiz> findByTeacherId(Long teacherId);

    List<Quiz> findByClassRoomIdAndPublished(Long classRoomId, Boolean published);
}