package com.profilewebsite.finalproject.controller;


import com.profilewebsite.finalproject.model.*;
import com.profilewebsite.finalproject.security.CustomUserDetails;
import com.profilewebsite.finalproject.service.ClassroomService;
import com.profilewebsite.finalproject.service.QuizService;
import com.profilewebsite.finalproject.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Transactional(readOnly = true)
    public String studentDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User student = userService.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Classroom> classes = new ArrayList<>(student.getStudentClasses());

        model.addAttribute("classes", classes);
        return "student";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam("code") String code,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Optional<Classroom> classroomOpt = classroomService.findByCode(code);

        if (classroomOpt.isEmpty()) {
            return "redirect:/student?error=invalid";
        }

        Classroom classroom = classroomOpt.get();
        User student = userDetails.getUser();

        // Check if already joined
        if (classroomService.isStudentInClass(classroom, student)) {
            return "redirect:/student?error=already_joined";
        }

        classroomService.addStudentToClass(classroom, student);

        return "redirect:/student?success";
    }

    @GetMapping("/student_class")
    public String viewClasses(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User student = userDetails.getUser();
        List<Classroom> classes = new ArrayList<>(student.getStudentClasses());

        model.addAttribute("classes", classes);
        return "student_class";
    }

    @GetMapping("/class/{classId}/quiz/{quizId}")
    public String takeQuiz(@PathVariable Long classId,
                           @PathVariable Long quizId,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {

        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        Classroom classroom = classroomService.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found"));

        // Check if student is in the class
        if (!classroomService.isStudentInClass(classroom, userDetails.getUser())) {
            return "redirect:/student?error=unauthorized";
        }

        model.addAttribute("quiz", quiz);
        return "studentquiz";
    }

    @GetMapping("/student_class/{classId}/quiz/{quizId}")
    public String takeQuizAlt(@PathVariable Long classId,
                              @PathVariable Long quizId,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              Model model) {
        return takeQuiz(classId, quizId, userDetails, model);
    }

    @PostMapping("/student_class/{classId}/quiz/{quizId}/submit")
    public String submitQuiz(@PathVariable Long classId,
                             @PathVariable Long quizId,
                             @AuthenticationPrincipal CustomUserDetails userDetails,
                             HttpServletRequest request) {

        Quiz quiz = quizService.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // Collect answers from form
        Map<String, String> answers = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();

        while (parameterNames.hasMoreElements()) {
            String paramName = parameterNames.nextElement();
            if (paramName.startsWith("q_")) {
                answers.put(paramName, request.getParameter(paramName));
            }
        }

        // Submit quiz
        quizService.submitQuiz(quiz, userDetails.getUser(), answers);

        return "redirect:/student?submitted";
    }

    @PostMapping("/class/{classId}/quiz/{quizId}/submit")
    public String submitQuizAlt(@PathVariable Long classId,
                                @PathVariable Long quizId,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                HttpServletRequest request) {
        return submitQuiz(classId, quizId, userDetails, request);
    }
}