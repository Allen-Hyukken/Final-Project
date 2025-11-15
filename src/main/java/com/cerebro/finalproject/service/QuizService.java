package com.cerebro.finalproject.service;

import com.cerebro.finalproject.model.*;
import com.cerebro.finalproject.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private ChoiceRepository choiceRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private AnswerRepository answerRepository;

    public Quiz createQuiz(String title, String description, Classroom classroom, User teacher) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setClassRoom(classroom);
        quiz.setTeacher(teacher);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setTotalPoints(0.0); // Will be updated as questions are added
        return quizRepository.save(quiz);
    }

    public Optional<Quiz> findById(Long id) {
        return quizRepository.findById(id);
    }

    public List<Quiz> findByClassRoomId(Long classRoomId) {
        return quizRepository.findByClassRoomId(classRoomId);
    }

    public Quiz updateQuiz(Quiz quiz) {
        return quizRepository.save(quiz);
    }

    @Transactional
    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }

    @Transactional
    public Question addQuestion(Quiz quiz, Question.QuestionType type, String text, String correctAnswer) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setType(type);
        question.setText(text);
        question.setCorrectAnswer(correctAnswer);
        question.setQIndex(quiz.getQuestions().size());

        Question savedQuestion = questionRepository.save(question);

        // Update total points
        updateQuizTotalPoints(quiz.getId());

        return savedQuestion;
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text, List<String> choiceTexts, String correctChoiceText) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setType(Question.QuestionType.MCQ);
        question.setText(text);
        question.setQIndex(quiz.getQuestions().size());
        question = questionRepository.save(question);

        // Add choices
        for (String choiceText : choiceTexts) {
            if (choiceText != null && !choiceText.trim().isEmpty()) {
                Choice choice = new Choice();
                choice.setQuestion(question);
                choice.setText(choiceText.trim());
                choice.setCorrect(choiceText.trim().equalsIgnoreCase(correctChoiceText.trim()));
                choiceRepository.save(choice);
            }
        }

        // Update total points
        updateQuizTotalPoints(quiz.getId());

        return question;
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        if (questionOpt.isPresent()) {
            Question question = questionOpt.get();
            Long quizId = question.getQuiz().getId();

            // Delete the question (choices will be deleted automatically due to cascade)
            questionRepository.deleteById(questionId);

            // Flush to ensure deletion is complete
            questionRepository.flush();

            // Update total points after deletion
            updateQuizTotalPoints(quizId);
        }
    }

    /**
     * Updates the total points for a quiz based on number of questions
     */
    private void updateQuizTotalPoints(Long quizId) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            // Refresh the quiz to get the latest questions list
            quizRepository.flush();

            // Reload the quiz from database to get fresh question list
            Quiz refreshedQuiz = quizRepository.findById(quizId).orElse(quiz);

            // Each question is worth 1 point
            refreshedQuiz.setTotalPoints((double) refreshedQuiz.getQuestions().size());
            quizRepository.save(refreshedQuiz);
        }
    }

    @Transactional
    public Attempt submitQuiz(Quiz quiz, User student, Map<String, String> answers) {
        // Create attempt
        Attempt attempt = new Attempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt = attemptRepository.save(attempt);

        double totalScore = 0;
        int totalQuestions = quiz.getQuestions().size();

        // Process each question
        for (Question question : quiz.getQuestions()) {
            String answerKey = "q_" + question.getId();
            String givenAnswer = answers.get(answerKey);

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;

            // Evaluate answer based on question type
            switch (question.getType()) {
                case MCQ:
                    if (givenAnswer != null && !givenAnswer.trim().isEmpty()) {
                        try {
                            Long choiceId = Long.parseLong(givenAnswer);
                            Optional<Choice> choiceOpt = choiceRepository.findById(choiceId);
                            if (choiceOpt.isPresent()) {
                                Choice selectedChoice = choiceOpt.get();
                                answer.setChoice(selectedChoice);
                                isCorrect = selectedChoice.getCorrect();
                            }
                        } catch (NumberFormatException e) {
                            // Invalid answer format, mark as incorrect
                            isCorrect = false;
                        }
                    }
                    break;

                case TF:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = givenAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                    }
                    break;

                case IDENT:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = givenAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                    }
                    break;

                case ESSAY:
                    answer.setGivenText(givenAnswer);
                    isCorrect = false; // Essays need manual grading
                    break;
            }

            answer.setCorrect(isCorrect);
            answerRepository.save(answer);

            // Add to score if correct
            if (isCorrect) {
                totalScore += 1.0;
            }
        }

        // Save final score
        attempt.setScore(totalScore);
        return attemptRepository.save(attempt);
    }

    public List<Attempt> getQuizAttempts(Long quizId) {
        return attemptRepository.findByQuizId(quizId);
    }

    public double calculateAverageScore(Long quizId) {
        List<Attempt> attempts = attemptRepository.findByQuizId(quizId);
        if (attempts.isEmpty()) {
            return 0.0;
        }

        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isEmpty()) {
            return 0.0;
        }

        Quiz quiz = quizOpt.get();
        double totalQuestions = quiz.getTotalPoints(); // Total points = number of questions

        if (totalQuestions == 0) {
            return 0.0;
        }

        // Calculate average percentage
        double sum = attempts.stream()
                .mapToDouble(a -> (a.getScore() / totalQuestions) * 100)
                .sum();

        return sum / attempts.size();
    }

    /**
     * Check if a student has already attempted a quiz
     */
    public boolean hasStudentAttempted(Long quizId, Long studentId) {
        return attemptRepository.existsByQuizIdAndStudentId(quizId, studentId);
    }

    /**
     * Get student's latest attempt for a quiz
     */
    public Optional<Attempt> getStudentLatestAttempt(Long quizId, Long studentId) {
        return attemptRepository.findFirstByQuizIdAndStudentIdOrderBySubmittedAtDesc(quizId, studentId);
    }
}