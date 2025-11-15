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
        return questionRepository.save(question);
    }

    @Transactional
    public Question addQuestionWithChoices(Quiz quiz, String text, List<String> choiceTexts, String correctChoiceText) {
        Question question = new Question();
        question.setQuiz(quiz);
        question.setType(Question.QuestionType.MCQ);
        question.setText(text);
        question.setQIndex(quiz.getQuestions().size());
        question = questionRepository.save(question);

        for (String choiceText : choiceTexts) {
            if (choiceText != null && !choiceText.trim().isEmpty()) {
                Choice choice = new Choice();
                choice.setQuestion(question);
                choice.setText(choiceText);
                choice.setCorrect(choiceText.equalsIgnoreCase(correctChoiceText));
                choiceRepository.save(choice);
            }
        }

        return question;
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    @Transactional
    public Attempt submitQuiz(Quiz quiz, User student, Map<String, String> answers) {
        Attempt attempt = new Attempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt = attemptRepository.save(attempt);

        double totalScore = 0;

        for (Question question : quiz.getQuestions()) {
            String answerKey = "q_" + question.getId();
            String givenAnswer = answers.get(answerKey);

            Answer answer = new Answer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);

            boolean isCorrect = false;

            switch (question.getType()) {
                case MCQ:
                    if (givenAnswer != null) {
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
                    isCorrect = givenAnswer != null &&
                            givenAnswer.equalsIgnoreCase(question.getCorrectAnswer());
                    break;

                case IDENT:
                    answer.setGivenText(givenAnswer);
                    isCorrect = givenAnswer != null &&
                            givenAnswer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
                    break;

                case ESSAY:
                    answer.setGivenText(givenAnswer);
                    isCorrect = false; // Essays need manual grading
                    break;
            }

            answer.setCorrect(isCorrect);
            answerRepository.save(answer);

            if (isCorrect) {
                totalScore += 1;
            }
        }

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

        Quiz quiz = quizRepository.findById(quizId).orElse(null);
        if (quiz == null) {
            return 0.0;
        }

        double totalQuestions = quiz.getQuestions().size();
        if (totalQuestions == 0) {
            return 0.0;
        }

        double sum = attempts.stream()
                .mapToDouble(a -> (a.getScore() / totalQuestions) * 100)
                .sum();

        return sum / attempts.size();
    }
}