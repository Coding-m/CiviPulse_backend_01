package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.*;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.security.JwtUtils;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final ComplaintRepository complaintRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    // ✅ Cryptographically secure OTP generator
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ================= SIGNUP =================
    public String signup(CitizenSignupRequest req) {
        if (citizenRepository.findByEmail(req.getEmail()) != null) {
            throw new BadRequestException("Email already registered");
        }

        Citizen citizen = Citizen.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.CITIZEN)
                .build();

        citizenRepository.save(citizen);
        log.info("New citizen registered: {}", req.getEmail());
        return "Citizen registered successfully";
    }

    // ================= LOGIN =================
    public CitizenLoginResponse login(LoginRequest request) {
        Citizen citizen = citizenRepository.findByEmail(request.getEmail());
        if (citizen == null || !passwordEncoder.matches(request.getPassword(), citizen.getPassword())) {
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(citizen);
        log.info("Citizen logged in: {}", request.getEmail());
        return new CitizenLoginResponse("Login successful", token, citizen.getRole().name());
    }

    // ================= FORGOT PASSWORD =================
    public String forgotPassword(String email) {
        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            // ✅ Don't reveal whether email exists — security best practice
            log.warn("Forgot password requested for unknown email: {}", email);
            return "If this email is registered, an OTP has been sent";
        }

        // ✅ SecureRandom instead of Math.random()
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        citizen.setResetToken(otp);
        citizen.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        citizenRepository.save(citizen);

        try {
            emailService.sendOtpEmail(email, otp);
            log.info("OTP sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send OTP email. Please try again later.");
        }

        return "If this email is registered, an OTP has been sent";
    }

    // ================= RESET PASSWORD =================
    public String resetPassword(String email, String resetToken, String newPassword) {
        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            throw new ResourceNotFoundException("Citizen not found");
        }

        if (citizen.getResetToken() == null ||
                !citizen.getResetToken().equals(resetToken) ||
                citizen.getResetTokenExpiry() == null ||
                citizen.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        citizen.setPassword(passwordEncoder.encode(newPassword));
        citizen.setResetToken(null);
        citizen.setResetTokenExpiry(null);
        citizenRepository.save(citizen);

        log.info("Password reset successful for: {}", email);
        return "Password reset successful";
    }

    // ================= SUBMIT COMPLAINT =================
    public CitizenComplaintResponse submitComplaint(Long citizenId, Complaint complaint) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found"));

        complaint.setCitizen(citizen);
        complaint.setSubmissionDate(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setComplaintStage(ComplaintStage.REGISTERED); // ✅ was missing

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint {} submitted by citizen {}", saved.getId(), citizenId);

        sendNotification(citizen.getEmail(), saved.getId(), saved.getStatus().name());

        return mapToCitizenResponse(saved);
    }

    // ================= GET MY COMPLAINTS =================
    public List<CitizenComplaintResponse> getMyComplaints(Long citizenId) {
        Citizen citizen = citizenRepository.findById(citizenId)
                .orElseThrow(() -> new ResourceNotFoundException("Citizen not found"));

        return complaintRepository.findByCitizen(citizen)
                .stream()
                .map(this::mapToCitizenResponse)
                .toList();
    }

    // ================= GET COMPLAINT DETAILS =================
    public CitizenComplaintResponse getComplaintDetails(Long citizenId, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new BadRequestException("Access denied");
        }

        return mapToCitizenResponse(complaint);
    }

    // ================= REPLY TO COMPLAINT =================
    public String replyToComplaint(Long citizenId, Long complaintId, String replyMessage) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new BadRequestException("Access denied");
        }

        checkIfComplaintDeleted(complaint);

        complaint.setClarificationMessage(replyMessage);
        complaintRepository.save(complaint);
        log.info("Citizen {} replied to complaint {}", citizenId, complaintId);

        sendNotification(complaint.getCitizen().getEmail(), complaint.getId(), "Citizen replied");

        return "Reply submitted successfully";
    }

    // ================= UPDATE MY PROFILE =================
    public CitizenProfileResponse updateMyProfile(
            Citizen citizen,
            CitizenProfileUpdateRequest request
    ) {
        if (request.getName() != null && !request.getName().isBlank()) {
            citizen.setName(request.getName());
        }
        if (request.getPhoneNo() != null && !request.getPhoneNo().isBlank()) {
            citizen.setPhoneNo(request.getPhoneNo());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            citizen.setAddress(request.getAddress());
        }
        if (request.getAge() > 0) {
            citizen.setAge(request.getAge());
        }

        citizenRepository.save(citizen);
        log.info("Profile updated for citizen: {}", citizen.getEmail());

        return CitizenProfileResponse.builder()
                .name(citizen.getName())
                .email(citizen.getEmail())
                .phoneNo(citizen.getPhoneNo())
                .address(citizen.getAddress())
                .age(citizen.getAge())
                .build();
    }

    // ================= HELPER: SEND NOTIFICATION =================
    private void sendNotification(String email, Long complaintId, String message) {
        try {
            NotificationDto payload = new NotificationDto(complaintId, message);
            messagingTemplate.convertAndSendToUser(
                    email.toLowerCase(), "/queue/notify", payload);
            messagingTemplate.convertAndSend("/topic/admin/complaints", payload);
        } catch (Exception e) {
            // ✅ Non-fatal — don't fail the main operation if WebSocket fails
            log.warn("Failed to send notification to {}: {}", email, e.getMessage());
        }
    }

    // ================= HELPER: MAP TO DTO =================
    private CitizenComplaintResponse mapToCitizenResponse(Complaint c) {
        return CitizenComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .complaintStage(c.getComplaintStage() != null ? c.getComplaintStage().name() : null)
                .officerRemark(c.getOfficerRemark())
                .deleted(c.isDeleted())
                .deletionReason(c.getDeletionReason())
                .clarificationMessage(c.getClarificationMessage())
                .adminRemark(c.getAdminRemark())
                .officerEvidenceUrl(c.getOfficerEvidenceUrl())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .submissionDate(c.getSubmissionDate())
                .build();
    }

    // ================= HELPER: CHECK DELETED =================
    private void checkIfComplaintDeleted(Complaint complaint) {
        if (complaint.isDeleted()) {
            throw new BadRequestException("Cannot reply or modify a deleted complaint");
        }
    }
}