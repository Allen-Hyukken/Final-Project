package com.profilewebsite.finalproject.controller;


import com.profilewebsite.finalproject.model.*;
import com.profilewebsite.finalproject.security.CustomUserDetails;
import com.profilewebsite.finalproject.service.ClassroomService;
import com.profilewebsite.finalproject.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private QuizService quizService;

    @GetMapping
    public String teacherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Classroom> classes = classroomService.findByTeacherId(userDetails.getId());
        model.addAttribute("classes", classes);
        return "teacher";
    }

    @PostMapping("/create")
    public String createClass(@RequestParam("name") String name,
                              @RequestParam(value = "banner", required = false) MultipartFile banner,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        classroomService.createClass(name, userDetails.getUser(), banner);
        return "redirect:/teacher?success";
    }

    @GetMapping("/class/{classId}")
    public String viewClass(@PathVariable Long classId,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        Classroom classroom = classroomService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Check if teacher owns this class
        if (!classroom.getTeacher().getId().equals(userDetails.getId())) {
            return "redirect:/teacher?error=unauthorized";
        }

        List<Quiz> quizzes = quizService.findByClassRoomId(classId);

        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("students", classroom.getStudents());

        return "teacher_classlist";
    }

    @GetMapping("/class/{classId}/create_quiz")
    public String showCreateQuizForm(@PathVariable Long classId,
                                     @AuthenticationPrincipal CustomUserDetails userDetails,
                                     Model model) {
        Classroom classroom = classroomService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        model.addAttribute("classRoom", classroom);
        model.addAttribute("classes", Arrays.asList(classroom));

        return "create_quiz";
    }

    @PostMapping("/save_quiz")
    public String saveQuiz(@RequestParam("title") String title,
                           @RequestParam("description") String description,
                           @RequestParam("classId") Long classId,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {

        Classroom classroom = classroomService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        Quiz quiz = quizService.createQuiz(title, description, classroom, userDetails.getUser());

        return "redirect:/teacher/class/" + classId + "?quizCreated";
    }

    @PostMapping("/add_question")
    public String addQuestion(@RequestParam("type") String type,
                              @RequestParam("text") String text,
                              @RequestParam(value = "correct", required = false) String correct,
                              @RequestParam(value = "choice1", required = false) String choice1,
                              @RequestParam(value = "choice2", required = false) String choice2,
                              @RequestParam(value = "choice3", required = false) String choice3,
                              @RequestParam(value = "choice4", required = false) String choice4,
                              @RequestParam("quizId") Long quizId) {

        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Question.QuestionType questionType = Question.QuestionType.valueOf(type);

        if (questionType == Question.QuestionType.MCQ) {
            List<String> choices = Arrays.asList(choice1, choice2, choice3, choice4);
            quizService.addQuestionWithChoices(quiz, text, choices, correct);
        } else {
            quizService.addQuestion(quiz, questionType, text, correct);
        }

        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{quizId}/edit")
    public String editQuiz(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        model.addAttribute("quiz", quiz);
        return "teacher_quizedit";
    }

    @PostMapping("/teacher_classlist/update")
    public String updateQuiz(@ModelAttribute Quiz quiz) {
        quizService.updateQuiz(quiz);
        return "redirect:/teacher/class/" + quiz.getClassRoom().getId();
    }

    @GetMapping("/quiz/{quizId}/results")
    public String viewQuizResults(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<Attempt> attempts = quizService.getQuizAttempts(quizId);
        double averageScore = quizService.calculateAverageScore(quizId);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        model.addAttribute("averageScore", averageScore);

        return "teacher_insidequiz_result";
    }

    @GetMapping("/quiz/{quizId}/delete")
    public String deleteQuiz(@PathVariable Long quizId,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Long classId = quiz.getClassRoom().getId();
        quizService.deleteQuiz(quizId);

        return "redirect:/teacher/class/" + classId + "?deleted";
    }

    @GetMapping("/teacher_classlist/delete_question/{questionId}")
    public String deleteQuestion(@PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return "redirect:/teacher/quiz/edit";
    }

    @PostMapping("/quiz/update")
    public String updateQuizPost(@ModelAttribute Quiz quiz) {
        quizService.updateQuiz(quiz);
        return "redirect:/teacher/class/" + quiz.getClassRoom().getId();
    }
}