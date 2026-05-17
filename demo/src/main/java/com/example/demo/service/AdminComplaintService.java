package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.payload.*;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;

import java.util.List;
import java.time.LocalDateTime;


@Slf4j  // ✅ Adds logger
@Service
@RequiredArgsConstructor
public class AdminComplaintService {

    private final ComplaintRepository complaintRepository;
    private final OfficerRepository officerRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // -------- HELPER: Send notification safely --------
    private void notify(String email, Long complaintId, String message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    email, "/queue/notify",
                    new NotificationDto(complaintId, message)
            );
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}", email, e.getMessage());
        }
    }

    // -------- GET OFFICER BY ID --------
    public Officer getOfficerById(Long officerId) {
        return officerRepository.findById(officerId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found with id: " + officerId));
    }

    // -------- LIST ALL COMPLAINTS (DB-level filtering) --------
    public List<Complaint> listAllComplaints(String search, String status, String priority) {
        log.info("Fetching complaints - search={}, status={}, priority={}", search, status, priority);
        return complaintRepository.findAllFiltered(search, status, priority); // ✅ DB query
    }

    // -------- GET COMPLAINT DETAILS --------
    public Complaint getComplaintDetails(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found with id: " + complaintId));
        if (complaint.isDeleted()) {
            throw new BadRequestException("Complaint has been deleted");
        }
        return complaint;
    }

    // -------- ASSIGN OFFICER --------
    @Transactional
    public Complaint assignOfficer(Long complaintId, ComplaintAssignRequestDto request) {
        log.info("Assigning officer {} to complaint {}", request.getOfficerId(), complaintId);

        Complaint complaint = getComplaintDetails(complaintId);
        Officer officer = getOfficerById(request.getOfficerId());

        if (complaint.getCategory() != officer.getDepartment()) {
            throw new BadRequestException("Officer department does not match complaint category");
        }
        if (officer.getStatus() != OfficerStatus.AVAILABLE) {
            throw new BadRequestException("Officer is not available");
        }

        complaint.setAssignedOfficer(officer);
        complaint.setAssignedDate(LocalDateTime.now());
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint.setComplaintStage(ComplaintStage.ASSIGNED);
        officer.setStatus(OfficerStatus.BUSY);

        officerRepository.save(officer);
        Complaint saved = complaintRepository.save(complaint);

        notify(saved.getCitizen().getEmail(), saved.getId(),
                "Assigned to Officer: " + officer.getName());
        notify(officer.getEmail(), saved.getId(),
                "New complaint assigned to you: " + saved.getTitle());
        if (saved.getAssignedAdmin() != null) {
            notify(saved.getAssignedAdmin().getEmail(), saved.getId(),
                    "Officer " + officer.getName() + " assigned to: " + saved.getTitle());
        }

        log.info("Officer {} assigned to complaint {} successfully", officer.getId(), complaintId);
        return saved;
    }

    // -------- UPDATE STATUS --------
    @Transactional
    public Complaint updateStatus(Long complaintId, ComplaintStatusUpdateRequestDto request) {
        log.info("Updating status of complaint {} to {}", complaintId, request.getStatus());

        Complaint complaint = getComplaintDetails(complaintId);
        ComplaintStatus newStatus;
        try {
            newStatus = ComplaintStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + request.getStatus());
        }

        complaint.setStatus(newStatus);
        Officer officer = complaint.getAssignedOfficer();

        if (newStatus == ComplaintStatus.RESOLVED) {
            complaint.setResolutionDate(LocalDateTime.now());
            if (officer != null) {
                long active = complaintRepository.countByAssignedOfficerAndStatusIn(
                        officer, List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS));
                if (active == 0) officer.setStatus(OfficerStatus.AVAILABLE);
                officerRepository.save(officer);
            }
        }

        Complaint saved = complaintRepository.save(complaint);

        notify(saved.getCitizen().getEmail(), saved.getId(),
                "Your complaint status updated to: " + newStatus.name());
        if (saved.getAssignedAdmin() != null) {
            notify(saved.getAssignedAdmin().getEmail(), saved.getId(),
                    "Complaint status updated to: " + newStatus.name());
        }

        return saved;
    }

    // -------- UPDATE STAGE --------
    @Transactional
    public Complaint updateStage(Long complaintId, ComplaintStage stage) {
        log.info("Updating stage of complaint {} to {}", complaintId, stage);
        Complaint complaint = getComplaintDetails(complaintId);
        complaint.setComplaintStage(stage);
        Complaint saved = complaintRepository.save(complaint);

        notify(saved.getCitizen().getEmail(), saved.getId(),
                "Complaint stage updated to: " + stage.name());
        if (saved.getAssignedAdmin() != null) {
            notify(saved.getAssignedAdmin().getEmail(), saved.getId(),
                    "Stage updated to: " + stage.name());
        }
        return saved;
    }

    // -------- UPDATE PRIORITY --------
    @Transactional
    public Complaint updatePriority(Long complaintId, String priority) {
        log.info("Updating priority of complaint {} to {}", complaintId, priority);
        Complaint complaint = getComplaintDetails(complaintId);

        Priority newPriority;
        try {
            newPriority = Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid priority value: " + priority);
        }

        complaint.setPriority(newPriority);
        Complaint saved = complaintRepository.save(complaint);

        notify(saved.getCitizen().getEmail(), saved.getId(),
                "Complaint priority updated to: " + newPriority.name());
        if (saved.getAssignedAdmin() != null) {
            notify(saved.getAssignedAdmin().getEmail(), saved.getId(),
                    "Priority updated to: " + newPriority.name());
        }
        return saved;
    }

    // -------- SOFT DELETE --------
    @Transactional
    public void deleteComplaint(Long complaintId, ComplaintDeleteRequestDto request) {
        log.info("Soft deleting complaint {}", complaintId);
        Complaint complaint = getComplaintDetails(complaintId);

        String reason = (request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason() : "No reason provided";

        complaint.setDeleted(true);
        complaint.setAdminRemark(reason);
        complaint.setResolutionDate(LocalDateTime.now());
        complaintRepository.save(complaint);

        notify(complaint.getCitizen().getEmail(), complaint.getId(),
                "Complaint deleted by admin. Reason: " + reason);
        if (complaint.getAssignedAdmin() != null) {
            notify(complaint.getAssignedAdmin().getEmail(), complaint.getId(),
                    "Complaint deleted. Reason: " + reason);
        }
    }

    // -------- OFFICER WORKLOAD (Single query) --------
    public List<OfficerWorkloadResponse> getAllOfficersWorkload() {
        log.info("Fetching all officers workload");
        return officerRepository.fetchAllOfficersWorkload(
            List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS)
        );
    }
}