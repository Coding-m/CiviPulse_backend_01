package com.example.demo.controller;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.ComplaintRequestDTO;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CitizenRepository citizenRepository; // ✅ Only for citizen lookup

    // ================== HELPER ==================
    // ✅ Authentication from Spring Security — no manual JWT parsing
    private Citizen getCitizen(Authentication authentication) {
        String email = authentication.getName();
        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            throw new ResourceNotFoundException("Citizen not found: " + email);
        }
        return citizen;
    }

    // ================= CREATE COMPLAINT =================
    @PostMapping(value = "/complaints/submit", consumes = "multipart/form-data")
    public ResponseEntity<Complaint> createComplaint(
            Authentication authentication,
            @ModelAttribute ComplaintRequestDTO dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        Citizen citizen = getCitizen(authentication);
        Complaint saved = complaintService.createFromDto(dto, citizen, image);
        log.info("Complaint submitted, id={}, citizen={}", saved.getId(), citizen.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ================= GET MY COMPLAINTS =================
    @GetMapping("/complaints")
    public ResponseEntity<List<Complaint>> getMyComplaints(Authentication authentication) {
        Citizen citizen = getCitizen(authentication);
        List<Complaint> complaints = complaintService.getComplaintsByCitizen(citizen);
        log.info("Fetched {} complaints for citizen={}", complaints.size(), citizen.getEmail());
        return ResponseEntity.ok(complaints);
    }

    // ================= GET SINGLE COMPLAINT =================
    @GetMapping("/complaints/{id}")
    public ResponseEntity<Complaint> getComplaintById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Citizen citizen = getCitizen(authentication);
        Complaint complaint = complaintService.getComplaintById(id, citizen);
        log.info("Fetched complaint id={} for citizen={}", id, citizen.getEmail());
        return ResponseEntity.ok(complaint);
    }

    // ================= UPDATE COMPLAINT =================
    @PutMapping(value = "/complaints/{id}", consumes = "multipart/form-data")
    public ResponseEntity<Complaint> updateComplaint(
            Authentication authentication,
            @PathVariable Long id,
            @ModelAttribute ComplaintRequestDTO dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        Citizen citizen = getCitizen(authentication);
        Complaint updated = complaintService.updateFromDto(citizen.getId(), id, dto, image);
        log.info("Complaint updated, id={}, citizen={}", updated.getId(), citizen.getEmail());
        return ResponseEntity.ok(updated);
    }

    // ================= DELETE COMPLAINT =================
    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<String> deleteComplaint(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Citizen citizen = getCitizen(authentication);
        complaintService.deleteComplaint(citizen.getId(), id);
        log.info("Complaint deleted, id={}, citizen={}", id, citizen.getEmail());
        return ResponseEntity.ok("Complaint deleted successfully");
    }
}