package com.example.demo.controller;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.*;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.service.CitizenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CitizenController {

    private final CitizenService citizenService;
    private final CitizenRepository citizenRepository; // ✅ Only for profile lookup helper

    // ================== HELPER ==================
    // ✅ Uses Authentication from Spring Security — no manual JWT parsing
    private Citizen getCitizen(Authentication authentication) {
        String email = authentication.getName();
        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            throw new ResourceNotFoundException("Citizen not found: " + email);
        }
        return citizen;
    }

    // ================== AUTH ==================

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody CitizenSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citizenService.signup(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CitizenLoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(citizenService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email must not be blank");
        }
        return ResponseEntity.ok(citizenService.forgotPassword(email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(
                citizenService.resetPassword(
                        request.getEmail(),
                        request.getResetToken(),
                        request.getNewPassword()
                )
        );
    }

    // ================== COMPLAINTS ==================

    @GetMapping("/complaints/my")
    public ResponseEntity<List<CitizenComplaintResponse>> getMyComplaints(
            Authentication authentication) {
        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.ok(citizenService.getMyComplaints(citizen.getId()));
    }

    @GetMapping("/complaints/deleted")
    public ResponseEntity<List<CitizenComplaintResponse>> getDeletedComplaints(
            Authentication authentication) {
        Citizen citizen = getCitizen(authentication);

        // ✅ Filter deleted complaints from the existing service call
        List<CitizenComplaintResponse> deleted = citizenService
                .getMyComplaints(citizen.getId())
                .stream()
                .filter(CitizenComplaintResponse::isDeleted)
                .collect(Collectors.toList());

        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/complaints/{complaintId}")
    public ResponseEntity<CitizenComplaintResponse> getComplaintDetails(
            Authentication authentication,
            @PathVariable Long complaintId) {
        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.ok(
                citizenService.getComplaintDetails(citizen.getId(), complaintId));
    }

    @PostMapping("/complaints")
    public ResponseEntity<CitizenComplaintResponse> submitComplaint(
            Authentication authentication,
            @RequestBody Complaint complaint) {
        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(citizenService.submitComplaint(citizen.getId(), complaint));
    }

    // ================== REPLY TO COMPLAINT ==================

    @PostMapping("/complaints/{complaintId}/reply")
    public ResponseEntity<String> replyToComplaint(
            Authentication authentication,
            @PathVariable Long complaintId,
            @RequestBody Map<String, String> body) {

        String message = body.get("message");
        if (message == null || message.isBlank()) {
            throw new BadRequestException("Reply message must not be blank");
        }

        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.ok(
                citizenService.replyToComplaint(citizen.getId(), complaintId, message));
    }

    // ================== PROFILE ==================

    @GetMapping("/profile")
    public ResponseEntity<CitizenProfileResponse> getMyProfile(
            Authentication authentication) {
        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.ok(
                CitizenProfileResponse.builder()
                        .name(citizen.getName())
                        .email(citizen.getEmail())
                        .phoneNo(citizen.getPhoneNo())
                        .address(citizen.getAddress())
                        .age(citizen.getAge())
                        .build()
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<CitizenProfileResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody CitizenProfileUpdateRequest updateRequest) {
        Citizen citizen = getCitizen(authentication);
        return ResponseEntity.ok(citizenService.updateMyProfile(citizen, updateRequest));
    }
}