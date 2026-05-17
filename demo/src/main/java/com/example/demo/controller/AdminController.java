package com.example.demo.controller;

import com.example.demo.entity.Admin;
import com.example.demo.entity.OfficerUpdateRequest;
import com.example.demo.exception.BadRequestException;
import com.example.demo.payload.*;
import com.example.demo.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // -------------------- SIGNUP --------------------
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody Admin admin) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.signup(admin));
    }

    // -------------------- LOGIN --------------------
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    // -------------------- CREATE OFFICER --------------------
    @PostMapping("/create-officer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createOfficer(@RequestBody OfficerSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createOfficer(request));
    }

    // -------------------- FORGOT PASSWORD --------------------
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(adminService.forgotPassword(request));
    }

    // -------------------- RESET PASSWORD --------------------
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(adminService.resetPassword(request));
    }

    // -------------------- RESET OFFICER PASSWORD --------------------
    @PostMapping("/reset-officer-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> resetOfficerPassword(
            @RequestBody ResetOfficerPasswordRequest request) {
        return ResponseEntity.ok(adminService.resetOfficerPassword(request));
    }

    // -------------------- GET PROFILE --------------------
    @GetMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProfileResponse> getMyProfile(Authentication authentication) {
        // ✅ Use service instead of casting principal directly — safer
        Admin admin = adminService.getAdminByEmail(authentication.getName());
        return ResponseEntity.ok(adminService.getMyProfile(admin));
    }

    // -------------------- UPDATE PROFILE --------------------
    @PutMapping("/profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateMyProfile(
            @RequestBody AdminProfileUpdateRequest request,
            Authentication authentication) {

        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Name must not be blank");
        }

        Admin admin = adminService.getAdminByEmail(authentication.getName());
        adminService.updateMyProfile(admin, request.getName());
        return ResponseEntity.ok("Admin profile updated successfully");
    }

    // -------------------- PENDING OFFICER UPDATE REQUESTS --------------------
    @GetMapping("/officer-update-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OfficerUpdateRequest>> getPendingOfficerUpdateRequests() {
        return ResponseEntity.ok(adminService.getPendingOfficerProfileRequests());
    }

    // -------------------- APPROVE OFFICER UPDATE --------------------
    @PutMapping("/officer-update-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveOfficerUpdate(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveOfficerProfileUpdate(id));
    }

    // -------------------- REJECT OFFICER UPDATE --------------------
    @PutMapping("/officer-update-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectOfficerUpdate(
            @PathVariable Long id,
            @RequestBody RejectRequestDto request) {

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BadRequestException("Rejection reason must not be blank");
        }

        return ResponseEntity.ok(
                adminService.rejectOfficerProfileUpdate(id, request.getReason()));
    }
}