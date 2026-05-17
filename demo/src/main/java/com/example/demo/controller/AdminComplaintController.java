package com.example.demo.controller;

import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStage;
import com.example.demo.payload.*;
import com.example.demo.service.AdminComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/complaints")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // ✅ Moved to class level — no need to repeat on every method
public class AdminComplaintController {

    private final AdminComplaintService adminComplaintService;

    // ---------------- LIST ALL COMPLAINTS ----------------
    @GetMapping
    public ResponseEntity<List<Complaint>> listComplaints(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(
                adminComplaintService.listAllComplaints(search, status, priority));
    }

    // ---------------- GET COMPLAINT BY ID ----------------
    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getComplaint(@PathVariable Long id) {
        return ResponseEntity.ok(adminComplaintService.getComplaintDetails(id));
    }

    // ---------------- ASSIGN OFFICER ----------------
    @PostMapping("/{id}/assign-officer")
    public ResponseEntity<Complaint> assignOfficer(
            @PathVariable Long id,
            @RequestBody ComplaintAssignRequestDto request) {
        // ✅ Removed redundant getOfficerById + setAssignedOfficer after save
        // — assignOfficer() in service already sets and saves the officer correctly
        return ResponseEntity.ok(adminComplaintService.assignOfficer(id, request));
    }

    // ---------------- UPDATE STATUS ----------------
    @PutMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(
            @PathVariable Long id,
            @RequestBody ComplaintStatusUpdateRequestDto request) {
        return ResponseEntity.ok(adminComplaintService.updateStatus(id, request));
    }

    // ---------------- UPDATE STAGE ----------------
    @PutMapping("/{id}/stage")
    public ResponseEntity<Complaint> updateStage(
            @PathVariable Long id,
            @RequestParam ComplaintStage stage) {
        return ResponseEntity.ok(adminComplaintService.updateStage(id, stage));
    }

    // ---------------- UPDATE PRIORITY ----------------
    @PutMapping("/{id}/priority")
    public ResponseEntity<Complaint> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String priority = body.get("priority");
        if (priority == null || priority.isBlank()) {
            throw new com.example.demo.exception.BadRequestException(
                    "Priority value must be provided");
        }
        return ResponseEntity.ok(adminComplaintService.updatePriority(id, priority));
    }

    // ---------------- SOFT DELETE ----------------
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteComplaint(
            @PathVariable Long id,
            @RequestBody ComplaintDeleteRequestDto request) {
        adminComplaintService.deleteComplaint(id, request);
        return ResponseEntity.ok("Complaint deleted and citizen notified successfully");
    }

    // ---------------- OFFICER WORKLOAD ----------------
    @GetMapping("/officers/workload")
    public ResponseEntity<List<OfficerWorkloadResponse>> getOfficersWorkload() {
        return ResponseEntity.ok(adminComplaintService.getAllOfficersWorkload());
    }
}