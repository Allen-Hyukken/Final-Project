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
        // FIXED: Use findByIdWithStudentClasses to eagerly fetch classes
        User student = userRepository.findByIdWithStudentClasses(userDetails.getUser().getId())
                .orElse(userDetails.getUser());

        model.addAttribute("classes", student.getStudentClasses());
        return "student";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam("code") String code,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<Classroom> classroomOpt = classroomService.findByCode(code);

        if (classroomOpt.isEmpty()) {
            return "redirect:/student?error";
        }

        Classroom classroom = classroomOpt.get();
        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);

        if (student == null) {
            return "redirect:/student?error";
        }

        // Check if already joined
        if (classroomService.isStudentInClass(classroom, student)) {
            return "redirect:/student?error";
        }

        classroomService.addStudentToClass(classroom, student);
        return "redirect:/student?success";
    }

    @GetMapping("/class/{classId}")
    public String viewClassQuizzes(@PathVariable Long classId, Model model) {
        Optional<Classroom> classroomOpt = classroomService.findById(classId);
        if (classroomOpt.isEmpty()) {
            return "redirect:/student";
        }

        Classroom classroom = classroomOpt.get();
        model.addAttribute("classRoom", classroom);
        model.addAttribute("quizzes", quizService.findByClassRoomId(classId));
        return "student_class";
    }

    @GetMapping("/class/{classId}/quiz/{quizId}")
    public String takeQuiz(@PathVariable Long classId,
                           @PathVariable Long quizId,
                           Model model) {
        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) {
            return "redirect:/student/class/" + classId;
        }

        model.addAttribute("quiz", quizOpt.get());
        return "studentquiz";
    }

    @PostMapping("/class/{classId}/quiz/{quizId}/submit")
    public String submitQuiz(@PathVariable Long classId,
                             @PathVariable Long quizId,
                             @RequestParam Map<String, String> answers,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        Optional<Quiz> quizOpt = quizService.findById(quizId);
        if (quizOpt.isEmpty()) {
            return "redirect:/student/class/" + classId;
        }

        User student = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (student == null) {
            return "redirect:/student/class/" + classId;
        }

        quizService.submitQuiz(quizOpt.get(), student, answers);

        return "redirect:/student/class/" + classId + "?submitted";
    }
}