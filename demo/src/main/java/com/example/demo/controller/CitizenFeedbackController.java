package com.example.demo.controller;

import com.example.demo.payload.FeedbackRequest;
import com.example.demo.service.CitizenFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/citizen/feedback")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class CitizenFeedbackController {

    private final CitizenFeedbackService feedbackService;

    public record ApiResponse(String message) {}

    // -------------------- SUBMIT FEEDBACK --------------------
    @PostMapping("/submit/{complaintId}")
    public ResponseEntity<ApiResponse> submitFeedback(
            Authentication authentication,
            @PathVariable Long complaintId,
            @Valid @RequestBody FeedbackRequest request
    ) {
        // ✅ Pass email from Authentication — no more SecurityContextHolder in service
        String result = feedbackService.submitFeedback(
                authentication.getName(), complaintId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse(result));
        // ✅ No more string-matching ("already submitted") to decide status code
        // — BadRequestException from service triggers 400 automatically via @ResponseStatus
    }

    // -------------------- GET FEEDBACK FOR A COMPLAINT --------------------
    @GetMapping("/complaint/{complaintId}")
    public ResponseEntity<?> getFeedbackForComplaint(@PathVariable Long complaintId) {
        // ✅ No try-catch needed — exceptions handled globally by @ResponseStatus
        return ResponseEntity.ok(feedbackService.getFeedbackByComplaint(complaintId));
    }

    // -------------------- GET MY FEEDBACKS --------------------
    @GetMapping("/my-feedback")
    public ResponseEntity<List<?>> getMyFeedback(Authentication authentication) {
        // ✅ Pass email from Authentication
        return ResponseEntity.ok(
                feedbackService.getFeedbackByCitizen(authentication.getName()));
    }
}