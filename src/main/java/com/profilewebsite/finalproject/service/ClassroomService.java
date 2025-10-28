package com.profilewebsite.finalproject.service;

import com.profilewebsite.finalproject.model.Classroom;
import com.profilewebsite.finalproject.model.User;
import com.profilewebsite.finalproject.repository.ClassroomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class ClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    private static final String UPLOAD_DIR = "uploads/banners/";

    public Classroom createClass(String name, User teacher, MultipartFile banner) {
        Classroom classroom = new Classroom();
        classroom.setName(name);
        classroom.setTeacher(teacher);
        classroom.setCode(generateUniqueCode());

        // Handle banner upload
        if (banner != null && !banner.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + banner.getOriginalFilename();
                Path uploadPath = Paths.get(UPLOAD_DIR);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                Files.copy(banner.getInputStream(), filePath);

                classroom.setBannerPath("/uploads/banners/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return classroomRepository.save(classroom);
    }

    public Optional<Classroom> findById(Long id) {
        return classroomRepository.findById(id);
    }

    public Optional<Classroom> findByCode(String code) {
        return classroomRepository.findByCode(code);
    }

    public List<Classroom> findByTeacherId(Long teacherId) {
        return classroomRepository.findByTeacherId(teacherId);
    }

    public Classroom addStudentToClass(Classroom classroom, User student) {
        classroom.getStudents().add(student);
        return classroomRepository.save(classroom);
    }

    public boolean isStudentInClass(Classroom classroom, User student) {
        return classroom.getStudents().contains(student);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateRandomCode(6);
        } while (classroomRepository.findByCode(code).isPresent());
        return code;
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }
}