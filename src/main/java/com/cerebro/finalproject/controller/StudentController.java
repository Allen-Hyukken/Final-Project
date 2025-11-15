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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String studentDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // Fetch user with eagerly loaded classes
        User student = userRepository.findByIdWithStudentClasses(userDetails.getUser().getId())
                .orElse(userDetails.getUser());

        model.addAttribute("classes", student.getStudentClasses());
        return "student";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam("code") String code,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {

        // Find classroom by code
        Optional<Classroom> classroomOpt = classroomService.findByCode(code);

        if (classroomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Invalid class code");
            return "redirect:/student?error";
        }

        Classroom classroom = classroomOpt.get();
        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);

        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "User not found");
            return "redirect:/student?error";
        }

        // Check if already joined
        if (classroomService.isStudentInClass(classroom, student)) {
            redirectAttributes.addFlashAttribute("error", "You have already joined this class");
            return "redirect:/student?error";
        }

        classroomService.addStudentToClass(classroom, student);
        redirectAttributes.addFlashAttribute("success", "Successfully joined the class!");
        return "redirect:/student?success";
    }

    @GetMapping("/class/{classId}")
    public String viewClassQuizzes(@PathVariable Long classId,
                                   @AuthenticationPrincipal CustomUserDetails userDetails,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Classroom not found");
            return "redirect:/student";
        }

        Classroom classroom = classroomOpt.get();
        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);

        // SECURITY: Verify student is enrolled in this class
        if (student == null || !classroomService.isStudentInClass(classroom, student)) {
            redirectAttributes.addFlashAttribute("error", "You are not enrolled in this class");
            return "redirect:/student";
        }

        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizService.findByClassRoomId(classId));
        model.addAttribute("studentId", student.getId());
        return "student_class";
    }

    @GetMapping("/class/{classId}/quiz/{quizId}")
    public String takeQuiz(@PathVariable Long classId,
                           @PathVariable Long quizId,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Verify classroom exists
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Classroom not found");
            return "redirect:/student";
        }

        Classroom classroom = classroomOpt.get();
        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);

        // SECURITY: Verify student is enrolled
        if (student == null || !classroomService.isStudentInClass(classroom, student)) {
            redirectAttributes.addFlashAttribute("error", "You are not enrolled in this class");
            return "redirect:/student";
        }

        // Verify quiz exists and belongs to the class
        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found");
            return "redirect:/student/class/" + classId;
        }

        Quiz quiz = quizOpt.get();

        // SECURITY: Verify quiz belongs to this classroom
        if (!quiz.getClassRoom().getId().equals(classId)) {
            redirectAttributes.addFlashAttribute("error", "Quiz does not belong to this class");
            return "redirect:/student/class/" + classId;
        }

        // CHECK: Prevent student from taking quiz if already attempted
        if (quizService.hasStudentAttempted(quizId, student.getId())) {
            Optional<Attempt> attemptOpt = quizService.getStudentLatestAttempt(quizId, student.getId());
            if (attemptOpt.isPresent()) {
                Attempt attempt = attemptOpt.get();
                redirectAttributes.addFlashAttribute("info",
                        "You have already completed this quiz. Your score: " +
                                attempt.getScore() + " / " + quiz.getTotalPoints());
            } else {
                redirectAttributes.addFlashAttribute("info", "You have already completed this quiz.");
            }
            return "redirect:/student/class/" + classId;
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("classId", classId);
        return "studentquiz";
    }

    @PostMapping("/class/{classId}/quiz/{quizId}/submit")
    public String submitQuiz(@PathVariable Long classId,
                             @PathVariable Long quizId,
                             @RequestParam Map<String, String> answers,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes) {

        // Verify classroom exists
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Classroom not found");
            return "redirect:/student";
        }

        Classroom classroom = classroomOpt.get();
        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);

        // SECURITY: Verify student is enrolled
        if (student == null || !classroomService.isStudentInClass(classroom, student)) {
            redirectAttributes.addFlashAttribute("error", "You are not enrolled in this class");
            return "redirect:/student";
        }

        // Verify quiz exists
        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Quiz not found");
            return "redirect:/student/class/" + classId;
        }

        Quiz quiz = quizOpt.get();

        // SECURITY: Verify quiz belongs to this classroom
        if (!quiz.getClassRoom().getId().equals(classId)) {
            redirectAttributes.addFlashAttribute("error", "Quiz does not belong to this class");
            return "redirect:/student/class/" + classId;
        }

        // CHECK: Prevent duplicate submission
        if (quizService.hasStudentAttempted(quizId, student.getId())) {
            redirectAttributes.addFlashAttribute("error", "You have already submitted this quiz.");
            return "redirect:/student/class/" + classId;
        }

        try {
            // Submit the quiz
            Attempt attempt = quizService.submitQuiz(quiz, student, answers);

            redirectAttributes.addFlashAttribute("success",
                    "Quiz submitted successfully! Your score: " + attempt.getScore() +
                            " out of " + quiz.getQuestions().size());

            return "redirect:/student/class/" + classId + "?submitted";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error submitting quiz. Please try again.");
            return "redirect:/student/class/" + classId + "/quiz/" + quizId;
        }
    }
}