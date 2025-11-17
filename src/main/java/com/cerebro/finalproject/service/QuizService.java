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
        quiz.setTotalPoints(0.0);
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
        return addQuestion(quiz, type, text, correctAnswer, 1.0);
    }

    @Transactional
    public Question addQuestion(Quiz quiz, Question.QuestionType type, String text, String correctAnswer, Double points) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setType(type);
        question.setText(text);
        question.setCorrectAnswer(correctAnswer);
        question.setQIndex(quiz.getQuestions().size());
        question.setPoints(points != null ? points : 1.0);

        Question savedQuestion = questionRepository.save(question);

        // Update total points based on all questions
        updateQuizTotalPoints(quiz.getId());

        return savedQuestion;
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text, List<String> choiceTexts, String correctChoiceText) {
        return addQuestionWithChoices(quiz, text, choiceTexts, correctChoiceText, 1.0);
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text, List<String> choiceTexts, String correctChoiceText, Double points) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setType(Question.QuestionType.MCQ);
        question.setText(text);
        question.setQIndex(quiz.getQuestions().size());
        question.setPoints(points != null ? points : 1.0);
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

            questionRepository.deleteById(questionId);
            questionRepository.flush();

            updateQuizTotalPoints(quizId);
        }
    }

    /**
     * Updates the total points for a quiz based on sum of all question points
     */
    private void updateQuizTotalPoints(Long quizId) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isPresent()) {
            Quiz quiz = quizOpt.get();
            quizRepository.flush();

            Quiz refreshedQuiz = quizRepository.findById(quizId).orElse(quiz);

            // Sum up all question points
            double totalPoints = refreshedQuiz.getQuestions().stream()
                    .mapToDouble(q -> q.getPoints() != null ? q.getPoints() : 1.0)
                    .sum();

            refreshedQuiz.setTotalPoints(totalPoints);
            quizRepository.save(refreshedQuiz);
        }
    }

    @Transactional
    public Attempt submitQuiz(Quiz quiz, User student, Map<String, String> answers) {
        Attempt attempt = new Attempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt = attemptRepository.save(attempt);

        double totalScore = 0;

        // Process each question
        for (Question question : quiz.getQuestions()) {
            String answerKey = "q_" + question.getId();
            String givenAnswer = answers.get(answerKey);

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;
            double questionPoints = question.getPoints() != null ? question.getPoints() : 1.0;

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
                            isCorrect = false;
                        }
                    }
                    break;

                case TF:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = normalizeAnswer(givenAnswer).equals(normalizeAnswer(question.getCorrectAnswer()));
                    }
                    break;

                case IDENT:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        isCorrect = normalizeAnswer(givenAnswer).equals(normalizeAnswer(question.getCorrectAnswer()));
                    }
                    break;

                case CODING:
                    answer.setGivenText(givenAnswer);
                    if (givenAnswer != null && question.getCorrectAnswer() != null) {
                        // Use advanced code comparison
                        isCorrect = compareCode(givenAnswer, question.getCorrectAnswer());
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
                totalScore += questionPoints;
            }
        }

        // Save final score
        attempt.setScore(totalScore);
        return attemptRepository.save(attempt);
    }

    /**
     * Normalize text answers by removing extra whitespace and converting to lowercase
     */
    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Advanced code comparison - normalizes whitespace, removes comments, and compares
     */
    private boolean compareCode(String studentCode, String correctCode) {
        if (studentCode == null || correctCode == null) {
            return false;
        }

        String normalizedStudent = normalizeCode(studentCode);
        String normalizedCorrect = normalizeCode(correctCode);

        return normalizedStudent.equals(normalizedCorrect);
    }


    private String normalizeCode(String code) {
        if (code == null) {
            return "";
        }

        // Remove single-line comments
        code = code.replaceAll("//.*?(\r?\n|$)", "\n");

        // Remove multi-line comments
        code = code.replaceAll("/\\*.*?\\*/", "");

        // Normalize whitespace: replace multiple spaces/tabs with single space
        code = code.replaceAll("[ \\t]+", " ");

        // Remove spaces around operators and punctuation for consistent comparison
        code = code.replaceAll("\\s*([{};(),=+\\-*/<>!&|])\\s*", "$1");

        // Remove leading/trailing whitespace from each line
        String[] lines = code.split("\r?\n");
        StringBuilder normalized = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                normalized.append(trimmed);
            }
        }

        // Convert to lowercase for case-insensitive comparison
        return normalized.toString().toLowerCase();
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
        double totalPoints = quiz.getTotalPoints();

        if (totalPoints == 0) {
            return 0.0;
        }

        // Calculate average percentage
        double sum = attempts.stream()
                .mapToDouble(a -> (a.getScore() / totalPoints) * 100)
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