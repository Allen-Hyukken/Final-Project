package com.cerebro.finalproject.controller;

import com.cerebro.finalproject.model.*;
import com.cerebro.finalproject.repository.UserRepository;
import com.cerebro.finalproject.security.CustomUserDetails;
import com.cerebro.finalproject.service.ClassroomService;
import com.cerebro.finalproject.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String teacherDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User teacher = userRepository.findByIdWithTeacherClasses(userDetails.getUser().getId())
                .orElse(userDetails.getUser());
        List<Classroom> classes = classroomService.findByTeacherId(teacher.getId());
        model.addAttribute("classes", classes);
        return "teacher";
    }

    @PostMapping("/create")
    public String createClass(@RequestParam("name") String name,
                              @RequestParam(value = "banner", required = false) MultipartFile banner,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        User teacher = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (teacher != null) {
            classroomService.createClass(name, teacher, banner);
        }
        return "redirect:/teacher";
    }

    @GetMapping("/class/{id}")
    public String viewClass(@PathVariable Long id, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(id);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Classroom classroom = classroomOpt.get();
        List<Quiz> quizzes = quizService.findByClassRoomId(id);

        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizzes);
        model.addAttribute("students", classroom.getStudents());
        return "teacher_classlist";
    }

    @GetMapping("/class/{classId}/create_quiz")
    public String showCreateQuizForm(@PathVariable Long classId, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        model.addAttribute("classRoom", classroomOpt.get());
        model.addAttribute("quiz", new Quiz());
        return "create_quiz";
    }

    @PostMapping("/class/{classId}/save_quiz")
    public String saveQuiz(@PathVariable Long classId,
                           @RequestParam("title") String title,
                           @RequestParam(value = "description", required = false) String description,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {

        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        User teacher = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (teacher == null) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizService.createQuiz(title, description, classroomOpt.get(), teacher);

        return "redirect:/teacher/quiz/" + quiz.getId() + "/edit";
    }

    @PostMapping("/quiz/{quizId}/add_question")
    public String addQuestion(@PathVariable Long quizId,
                              @RequestParam("type") String typeStr,
                              @RequestParam("text") String text,
                              @RequestParam(value = "correct", required = false) String correct,
                              @RequestParam(value = "choice1", required = false) String choice1,
                              @RequestParam(value = "choice2", required = false) String choice2,
                              @RequestParam(value = "choice3", required = false) String choice3,
                              @RequestParam(value = "choice4", required = false) String choice4) {

        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        Question.QuestionType type = Question.QuestionType.valueOf(typeStr);

        if (type == Question.QuestionType.MCQ) {
            List<String> choices = List.of(choice1, choice2, choice3, choice4);
            quizService.addQuestionWithChoices(quiz, text, choices, correct);
        } else {
            quizService.addQuestion(quiz, type, text, correct);
        }

        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{id}/edit")
    public String editQuiz(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        model.addAttribute("quiz", quizOpt.get());
        return "teacher_quizedit";
    }

    @PostMapping("/quiz/{id}/update")
    public String updateQuiz(@PathVariable Long id,
                             @RequestParam("title") String title,
                             @RequestParam(value = "description", required = false) String description) {

        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        quiz.setTitle(title);
        quiz.setDescription(description);
        quizService.updateQuiz(quiz);

        return "redirect:/teacher/class/" + quiz.getClassRoom().getId();
    }

    @GetMapping("/quiz/{id}/delete")
    public String deleteQuiz(@PathVariable Long id) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isPresent()) {
            Long classId = quizOpt.get().getClassRoom().getId();
            quizService.deleteQuiz(id);
            return "redirect:/teacher/class/" + classId;
        }
        return "redirect:/teacher";
    }

    // FIXED: Changed from @RequestParam to @PathVariable for both questionId and quizId
    @GetMapping("/quiz/{quizId}/question/{questionId}/delete")
    public String deleteQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        quizService.deleteQuestion(questionId);
        return "redirect:/teacher/quiz/" + quizId + "/edit";
    }

    @GetMapping("/quiz/{id}/results")
    public String viewQuizResults(@PathVariable Long id, Model model) {
        Optional<Quiz> quizOpt = quizService.findById(id);
        if (quizOpt.isEmpty()) {
            return "redirect:/teacher";
        }

        Quiz quiz = quizOpt.get();
        List<Attempt> attempts = quizService.getQuizAttempts(id);
        double averageScore = quizService.calculateAverageScore(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", attempts);
        model.addAttribute("averageScore", averageScore);

        return "teacher_insidequiz_result";
    }
}