package com.example.demo.controller;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.payload.ComplaintRequestDTO;
import com.example.demo.security.JwtUtils;
import com.example.demo.service.ComplaintService;
import com.example.demo.repositories.CitizenRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/citizen")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 🔥 for Vercel deployment
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CitizenRepository citizenRepository;
    private final JwtUtils jwtUtils;

    // ---------------- GET LOGGED-IN CITIZEN ----------------
    private Citizen getCitizenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtUtils.extractEmail(token);

        Citizen citizen = citizenRepository.findByEmail(email);

        if (citizen == null) {
            throw new RuntimeException("Citizen not found");
        }

        return citizen;
    }

    // ================= CREATE COMPLAINT =================
    @PostMapping(
            value = "/complaints/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Complaint> createComplaint(
            HttpServletRequest request,
            @ModelAttribute ComplaintRequestDTO dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {

        Citizen citizen = getCitizenFromRequest(request);

        Complaint saved = complaintService.createFromDto(dto, citizen, image);

        return ResponseEntity.ok(saved);
    }

    // ================= GET MY COMPLAINTS =================
    @GetMapping("/complaints")
    public ResponseEntity<Page<Complaint>> getMyComplaints(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {

        Citizen citizen = getCitizenFromRequest(request);

        Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(
                complaintService.getComplaintsByCitizen(citizen, pageable)
        );
    }

    // ================= GET SINGLE =================
    @GetMapping("/complaints/{id}")
    public ResponseEntity<Complaint> getComplaintById(
            HttpServletRequest request,
            @PathVariable Long id
    ) {

        Citizen citizen = getCitizenFromRequest(request);

        return ResponseEntity.ok(
                complaintService.getComplaintById(id, citizen)
        );
    }

    // ================= UPDATE =================
    @PutMapping(
            value = "/complaints/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Complaint> updateComplaint(
            HttpServletRequest request,
            @PathVariable Long id,
            @ModelAttribute ComplaintRequestDTO dto,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {

        Citizen citizen = getCitizenFromRequest(request);

        Complaint updated =
                complaintService.updateFromDto(citizen.getId(), id, dto, image);

        return ResponseEntity.ok(updated);
    }

    // ================= DELETE =================
    @DeleteMapping("/complaints/{id}")
    public ResponseEntity<String> deleteComplaint(
            HttpServletRequest request,
            @PathVariable Long id
    ) {

        Citizen citizen = getCitizenFromRequest(request);

        complaintService.deleteComplaint(citizen.getId(), id);

        return ResponseEntity.ok("✅ Complaint deleted successfully");
    }
}
