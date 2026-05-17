package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.*;
import com.example.demo.repositories.AdminRepository;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.repositories.OfficerUpdateRequestRepository;
import com.example.demo.security.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final OfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final OfficerUpdateRequestRepository officerUpdateRequestRepository;
    private final ObjectMapper objectMapper;

    // ✅ Cryptographically secure OTP generator
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // -------------------- Admin Signup --------------------
    public String signup(Admin admin) {
        if (adminRepository.findByEmail(admin.getEmail()) != null) {
            throw new BadRequestException("Admin email already exists");
        }

        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole(Role.ADMIN);
        adminRepository.save(admin);

        log.info("New admin registered: {}", admin.getEmail());
        return "Admin registered successfully";
    }

    // -------------------- Admin Login --------------------
    public LoginResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());

        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(admin.getEmail(), admin.getRole().name());
        log.info("Admin logged in: {}", request.getEmail());
        return new LoginResponse("Login successful", token, admin.getRole().name());
    }

    // -------------------- Create Officer --------------------
    public String createOfficer(OfficerSignupRequest request) {
        if (officerRepository.findByEmail(request.getEmail()) != null) {
            throw new BadRequestException("Officer email already exists");
        }

        ComplaintCategory department;
        try {
            department = ComplaintCategory.valueOf(request.getDepartment().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid department value: " + request.getDepartment());
        }

        Officer officer = Officer.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNo(request.getPhoneNo())
                .department(department)
                .address(request.getAddress())
                .age(request.getAge())
                .role(Role.OFFICER)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        officerRepository.save(officer);
        log.info("Officer created: {}", request.getEmail());

        try {
            emailService.sendCredentialsEmail(request.getEmail(), request.getPassword(), "Officer");
        } catch (MessagingException e) {
            // ✅ Non-fatal — officer is created, just email failed
            log.error("Officer created but email failed for {}: {}", request.getEmail(), e.getMessage());
            return "Officer created successfully but email sending failed";
        }

        return "Officer created successfully. Login credentials sent via email.";
    }

    // -------------------- Admin Forgot Password --------------------
    public String forgotPassword(ForgotPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());

        if (admin == null) {
            // ✅ Don't reveal whether email exists
            log.warn("Forgot password for unknown admin email: {}", request.getEmail());
            return "If this email is registered, an OTP has been sent";
        }

        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        admin.setResetToken(otp);
        admin.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        adminRepository.save(admin);

        try {
            emailService.sendOtpEmail(admin.getEmail(), otp);
            log.info("OTP sent to admin: {}", admin.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send OTP to {}: {}", admin.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }

        return "If this email is registered, an OTP has been sent";
    }

    // -------------------- Reset Admin Password --------------------
    public String resetPassword(ResetPasswordRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail());
        if (admin == null) {
            throw new ResourceNotFoundException("Admin not found");
        }

        if (admin.getResetToken() == null ||
                !admin.getResetToken().equals(request.getResetToken()) ||
                admin.getResetTokenExpiry() == null ||
                admin.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        admin.setResetToken(null);
        admin.setResetTokenExpiry(null);
        adminRepository.save(admin);

        log.info("Password reset successful for admin: {}", request.getEmail());
        return "Password reset successful";
    }

    // -------------------- Reset Officer Password --------------------
    public String resetOfficerPassword(ResetOfficerPasswordRequest request) {
        Officer officer = officerRepository.findByEmail(request.getOfficerEmail());
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found");
        }

        officer.setPassword(passwordEncoder.encode(request.getNewPassword()));
        officerRepository.save(officer);

        log.info("Password reset for officer: {}", request.getOfficerEmail());
        return "Officer password reset successfully";
    }

    // -------------------- Get Admin Profile --------------------
    public AdminProfileResponse getMyProfile(Admin admin) {
        return AdminProfileResponse.builder()
                .name(admin.getName())
                .email(admin.getEmail())
                .build();
    }

    // -------------------- Get Admin By Email --------------------
    public Admin getAdminByEmail(String email) {
        Admin admin = adminRepository.findByEmail(email);
        if (admin == null) {
            throw new ResourceNotFoundException("Admin not found");
        }
        return admin;
    }

    // -------------------- Update Admin Profile --------------------
    public void updateMyProfile(Admin admin, String newName) {
        if (newName != null && !newName.trim().isEmpty()) {
            admin.setName(newName.trim());
        }
        adminRepository.save(admin);
        log.info("Profile updated for admin: {}", admin.getEmail());
    }

    // -------------------- Pending Officer Profile Requests --------------------
    public List<OfficerUpdateRequest> getPendingOfficerProfileRequests() {
        return officerUpdateRequestRepository.findByStatus(RequestStatus.PENDING);
    }

    // -------------------- Approve Officer Profile Update --------------------
    public String approveOfficerProfileUpdate(Long requestId) {
        OfficerUpdateRequest request = officerUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Update request not found"));

        Officer officer = request.getOfficer();

        try {
            OfficerProfileUpdateRequest updateData = objectMapper.readValue(
                    request.getRequestedData(),
                    OfficerProfileUpdateRequest.class
            );

            if (updateData.getName() != null)    officer.setName(updateData.getName());
            if (updateData.getPhoneNo() != null)  officer.setPhoneNo(updateData.getPhoneNo());
            if (updateData.getAddress() != null)  officer.setAddress(updateData.getAddress());
            if (updateData.getAge() > 0)          officer.setAge(updateData.getAge());

            officerRepository.save(officer);

            request.setStatus(RequestStatus.APPROVED);
            request.setReviewedAt(LocalDateTime.now());
            officerUpdateRequestRepository.save(request);

            log.info("Officer profile update approved for request: {}", requestId);
            return "Officer profile update approved successfully";

        } catch (Exception e) {
            log.error("Failed to approve officer profile update {}: {}", requestId, e.getMessage());
            throw new RuntimeException("Failed to approve profile update");
        }
    }

    // -------------------- Reject Officer Profile Update --------------------
    public String rejectOfficerProfileUpdate(Long requestId, String reason) {
        OfficerUpdateRequest request = officerUpdateRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Update request not found"));

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectionReason(reason != null ? reason : "No reason provided");
        request.setReviewedAt(LocalDateTime.now());
        officerUpdateRequestRepository.save(request);

        log.info("Officer profile update rejected for request: {}", requestId);
        return "Officer profile update rejected";
    }
}