package com.example.demo.controller;

import com.example.demo.payload.FeedbackViewResponse;
import com.example.demo.service.OfficerFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/officer/feedback")
@RequiredArgsConstructor
@CrossOrigin("*")
public class OfficerFeedbackController {

    private final OfficerFeedbackService feedbackService;

    @GetMapping("/my-complaints")
    public ResponseEntity<List<FeedbackViewResponse>> getMyFeedbacks(Authentication authentication) {
        String email = authentication.getName(); // ✅ Pulled from JWT in controller
        return ResponseEntity.ok(feedbackService.getMyComplaintFeedbacks(email));
    }
}