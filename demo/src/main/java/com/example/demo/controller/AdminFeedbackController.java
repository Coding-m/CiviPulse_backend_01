package com.example.demo.controller;

import com.example.demo.payload.FeedbackViewResponse;
import com.example.demo.service.AdminFeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')") // ✅ Secure the endpoint
public class AdminFeedbackController {

    private final AdminFeedbackService feedbackService;

    @GetMapping("/all")
    public ResponseEntity<List<FeedbackViewResponse>> getAllFeedback() {
        log.info("Admin fetching all feedbacks");
        return ResponseEntity.ok(feedbackService.getAllFeedbacks());
    }
}