package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.*;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.service.OfficerComplaintService;
import com.example.demo.service.OfficerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/officer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OfficerComplaintController {

    private final OfficerComplaintService officerComplaintService;
    private final OfficerService officerService;
    private final OfficerRepository officerRepository; // ✅ Only kept for profile lookup

    // ================== HELPER ==================
    // ✅ Use Authentication from Spring Security — no manual JWT parsing
    private Officer getOfficer(Authentication authentication) {
        String email = authentication.getName();
        Officer officer = officerRepository.findByEmail(email);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found: " + email);
        }
        return officer;
    }

    // -------------------- LOGIN --------------------
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(officerService.login(request));
    }

    // -------------------- GET ASSIGNED COMPLAINTS --------------------
    @GetMapping("/complaints")
    public ResponseEntity<List<OfficerComplaintResponse>> getAssignedComplaints(
            Authentication authentication) {
        Officer officer = getOfficer(authentication);
        return ResponseEntity.ok(
                officerComplaintService.getAssignedComplaintResponses(officer));
    }

    // -------------------- UPDATE COMPLAINT --------------------
    @PutMapping("/complaints/{id}")
    public ResponseEntity<OfficerComplaintResponse> updateComplaint(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) String expectedDate
    ) {
        Officer officer = getOfficer(authentication);
        Complaint complaint = null;

        try {
            if (status != null && !status.isBlank()) {
                complaint = officerComplaintService.updateStatus(
                        officer, id, ComplaintStatus.valueOf(status.toUpperCase()));
            }
            if (stage != null && !stage.isBlank()) {
                complaint = officerComplaintService.updateStage(
                        officer, id, ComplaintStage.valueOf(stage.toUpperCase()));
            }
            if (remark != null && !remark.isBlank()) {
                complaint = officerComplaintService.addRemark(officer, id, remark);
            }
            if (expectedDate != null && !expectedDate.isBlank()) {
                complaint = officerComplaintService.updateExpectedCompletionDate(
                        officer, id, LocalDate.parse(expectedDate));
            }

            if (complaint == null) {
                throw new BadRequestException(
                        "At least one field (status, stage, remark, expectedDate) must be provided");
            }

            return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(complaint));

        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status or stage value provided");
        }
    }

    // -------------------- UPLOAD EVIDENCE --------------------
    @PostMapping("/complaints/{id}/evidence")
    public ResponseEntity<OfficerComplaintResponse> uploadEvidence(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File must not be empty");
        }

        Officer officer = getOfficer(authentication);
        Complaint updated = officerComplaintService.uploadEvidence(officer, id, file);
        return ResponseEntity.ok(officerComplaintService.mapToOfficerResponse(updated));
    }

    // -------------------- SINGLE COMPLAINT FEEDBACK --------------------
    @GetMapping("/{complaintId}/feedback")
    public ResponseEntity<OfficerFeedbackResponse> viewFeedback(
            Authentication authentication,
            @PathVariable Long complaintId
    ) {
        Officer officer = getOfficer(authentication);
        return ResponseEntity.ok(
                officerComplaintService.getFeedbackForOfficer(officer, complaintId));
    }

    // -------------------- ALL FEEDBACKS --------------------
    @GetMapping("/feedbacks")
    public ResponseEntity<List<OfficerFeedbackResponse>> viewAllFeedbacks(
            Authentication authentication) {
        Officer officer = getOfficer(authentication);
        return ResponseEntity.ok(
                officerComplaintService.getAllFeedbackForOfficer(officer));
    }

    // -------------------- OFFICER WORKLOAD --------------------
    @GetMapping("/workload")
    public ResponseEntity<List<OfficerWorkloadResponse>> getOfficerWorkload() {
        // ✅ Delegated to service which uses single JOIN query
        return ResponseEntity.ok(officerComplaintService.getAllOfficersWorkload());
    }

    // -------------------- GET PROFILE --------------------
    @GetMapping("/profile")
    public ResponseEntity<OfficerProfileUpdateResponse> getMyProfile(
            Authentication authentication) {
        Officer officer = getOfficer(authentication);
        return ResponseEntity.ok(
                OfficerProfileUpdateResponse.builder()
                        .name(officer.getName())
                        .email(officer.getEmail())
                        .phoneNo(officer.getPhoneNo())
                        .address(officer.getAddress())
                        .age(officer.getAge())
                        .build()
        );
    }

    // -------------------- REQUEST PROFILE UPDATE --------------------
    @PutMapping("/profile")
    public ResponseEntity<String> requestProfileUpdate(
            Authentication authentication,
            @RequestBody OfficerProfileUpdateRequest updateRequest
    ) {
        Officer officer = getOfficer(authentication);
        return ResponseEntity.ok(
                officerService.requestProfileUpdate(officer, updateRequest));
    }
}