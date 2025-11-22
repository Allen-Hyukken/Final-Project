package com.cerebro.finalproject.controller;

import com.cerebro.finalproject.model.Classroom;
import com.cerebro.finalproject.service.ClassroomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Controller
public class BannerController {

    @Autowired
    private ClassroomService classroomService;

    @GetMapping("/classroom/{id}/banner")
    public ResponseEntity<byte[]> getClassroomBanner(@PathVariable Long id) {
        Optional<Classroom> classroomOpt = classroomService.findById(id);

        if (classroomOpt.isPresent()) {
            Classroom classroom = classroomOpt.get();

            if (classroom.getBannerImage() != null) {
                HttpHeaders headers = new HttpHeaders();

                // Set content type
                if (classroom.getBannerContentType() != null) {
                    headers.setContentType(MediaType.parseMediaType(classroom.getBannerContentType()));
                } else {
                    headers.setContentType(MediaType.IMAGE_JPEG);
                }

                return new ResponseEntity<>(classroom.getBannerImage(), headers, HttpStatus.OK);
            }
        }

        // If no banner found, return default
        return getDefaultBanner();
    }

    private ResponseEntity<byte[]> getDefaultBanner() {
        try {
            ClassPathResource defaultBanner = new ClassPathResource("static/images/default-class.jpg");
            InputStream inputStream = defaultBanner.getInputStream();
            byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}