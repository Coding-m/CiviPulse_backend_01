package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.entity.*;
import com.example.demo.exception.AccessDeniedException;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ComplaintNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.NotificationDto;
import com.example.demo.payload.OfficerComplaintResponse;
import com.example.demo.payload.OfficerFeedbackResponse;
import com.example.demo.payload.OfficerWorkloadResponse;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.FeedbackRepository;
import com.example.demo.repositories.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OfficerComplaintService {

    private final ComplaintRepository complaintRepository;
    private final FeedbackRepository feedbackRepository;
    private final OfficerRepository officerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Cloudinary cloudinary; // ✅ Injected via CloudinaryConfig

    // ✅ Static constants — not instance fields
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_FILE_TYPES =
            List.of("image/png", "image/jpeg", "image/jpg");

    // ==================== HELPER ====================
    private Officer getManagedOfficer(Officer officer) {
        return officerRepository.findById(officer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Officer not found"));
    }

    // ==================== ASSIGNED COMPLAINTS ====================
    public List<OfficerComplaintResponse> getAssignedComplaintResponses(Officer officer) {
        Officer managedOfficer = getManagedOfficer(officer);
        return complaintRepository.findByAssignedOfficer(managedOfficer)
                .stream()
                .map(this::mapToOfficerResponse)
                .toList();
    }

    // ==================== UPLOAD EVIDENCE (Cloudinary) ====================
    public Complaint uploadEvidence(Officer officer, Long complaintId, MultipartFile file) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);

        if (file == null || file.isEmpty())
            throw new BadRequestException("File is empty");

        if (!ALLOWED_FILE_TYPES.contains(file.getContentType()))
            throw new BadRequestException("Only PNG/JPG images allowed");

        if (file.getSize() > MAX_FILE_SIZE)
            throw new BadRequestException("File size exceeds 5MB");

        try {
            // ✅ Upload to Cloudinary instead of local disk
            Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "smartcity/officer-evidence",
                    "resource_type", "image"
                )
            );
            String evidenceUrl = (String) uploadResult.get("secure_url");
            log.info("Evidence uploaded to Cloudinary for complaint {}: {}", complaintId, evidenceUrl);

            complaint.setOfficerEvidenceUrl(evidenceUrl);

            if (complaint.getStatus() == ComplaintStatus.PENDING) {
                complaint.setStatus(ComplaintStatus.IN_PROGRESS);
            }

            Complaint saved = complaintRepository.saveAndFlush(complaint);
            updateOfficerStatus(saved.getAssignedOfficer());

            notifyAdmins(saved, "Officer uploaded evidence");
            notifyCitizen(saved, "Officer uploaded evidence for your complaint");

            return saved;

        } catch (IOException e) {
            log.error("Evidence upload failed for complaint {}: {}", complaintId, e.getMessage());
            throw new RuntimeException("Evidence upload failed", e);
        }
    }

    // ==================== FEEDBACK ====================
    public OfficerFeedbackResponse getFeedbackForOfficer(Officer officer, Long complaintId) {
        Complaint complaint = getOfficerComplaint(officer, complaintId);
        Feedback feedback = feedbackRepository.findByComplaintId(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("No feedback submitted"));
        return mapToFeedbackResponse(complaint, feedback);
    }

    public List<OfficerFeedbackResponse> getAllFeedbackForOfficer(Officer officer) {
        Officer managedOfficer = getManagedOfficer(officer);
        return complaintRepository.findByAssignedOfficer(managedOfficer)
                .stream()
                .filter(c -> c.getFeedback() != null)
                .map(c -> mapToFeedbackResponse(c, c.getFeedback()))
                .toList();
    }

    private OfficerFeedbackResponse mapToFeedbackResponse(Complaint c, Feedback f) {
        return OfficerFeedbackResponse.builder()
                .complaintId(c.getId())
                .rating(f.getRating())
                .officerBehaviourRating(f.getOfficerBehaviourRating())
                .resolutionStatus(f.getResolutionStatus())
                .timeliness(f.getTimeliness())
                .feedbackComment(f.getFeedbackComment())
                .feedbackImageUrl(f.getFeedbackImageUrl())
                .reopened(f.getReopened())
                .feedbackSubmittedAt(f.getFeedbackSubmittedAt())
                .build();
    }

    // ==================== MAP RESPONSE ====================
    public OfficerComplaintResponse mapToOfficerResponse(Complaint c) {
        Officer o = c.getAssignedOfficer();

        // ✅ Use preloaded activeComplaints from officer entity
        // instead of firing a DB query per complaint
        long active = o != null ? o.getActiveComplaints() : 0;

        return OfficerComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(c.getCategory() != null ? c.getCategory().name() : null)
                .priority(c.getPriority() != null ? c.getPriority().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .description(c.getDescription())
                .location(c.getLocation())
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .submissionDate(c.getSubmissionDate())
                .imageUrl(c.getImageUrl())
                .officerEvidenceUrl(c.getOfficerEvidenceUrl())
                .resolutionDate(c.getResolutionDate())
                .complaintStage(c.getComplaintStage() != null ? c.getComplaintStage().name() : null)
                .assignedOfficerName(o != null ? o.getName() : null)
                .assignedOfficerStatus(
                        o != null && o.getStatus() != null ? o.getStatus().name() : null)
                .assignedOfficerDepartment(
                        o != null && o.getDepartment() != null ? o.getDepartment().name() : null)
                .assignedOfficerActiveComplaints(active)
                .officerRemark(c.getOfficerRemark())
                .assignedDate(c.getAssignedDate())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .build();
    }

    // ==================== VALIDATION ====================
    private Complaint getOfficerComplaint(Officer officer, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ComplaintNotFoundException("Complaint not found"));

        if (complaint.getAssignedOfficer() == null ||
                !complaint.getAssignedOfficer().getId().equals(officer.getId())) {
            throw new AccessDeniedException("You are not allowed to access this complaint");
        }

        return complaint;
    }

    // ==================== STATUS UPDATES ====================
    public Complaint updateStatus(Officer officer, Long id, ComplaintStatus status) {
        Complaint complaint = getOfficerComplaint(officer, id);
        complaint.setStatus(status);

        if (status == ComplaintStatus.RESOLVED) {
            complaint.setResolutionDate(LocalDateTime.now());
        }

        Complaint saved = complaintRepository.saveAndFlush(complaint);
        updateOfficerStatus(saved.getAssignedOfficer());

        notifyAdmins(saved, "Complaint status updated to: " + status.name());
        notifyCitizen(saved, "Your complaint status updated to: " + status.name());

        log.info("Complaint {} status updated to {} by officer {}", id, status, officer.getId());
        return saved;
    }

    public Complaint updateStage(Officer officer, Long id, ComplaintStage stage) {
        Complaint complaint = getOfficerComplaint(officer, id);
        complaint.setComplaintStage(stage);
        log.info("Complaint {} stage updated to {} by officer {}", id, stage, officer.getId());
        return complaintRepository.saveAndFlush(complaint);
    }

    public Complaint updateExpectedCompletionDate(Officer officer, Long id, LocalDate date) {
        Complaint complaint = getOfficerComplaint(officer, id);
        complaint.setExpectedCompletionDate(date);
        log.info("Complaint {} expected completion date set to {}", id, date);
        return complaintRepository.saveAndFlush(complaint);
    }

    public Complaint addRemark(Officer officer, Long id, String remark) {
        Complaint complaint = getOfficerComplaint(officer, id);
        complaint.setOfficerRemark(remark);
        log.info("Remark added to complaint {} by officer {}", id, officer.getId());
        return complaintRepository.saveAndFlush(complaint);
    }

    // ==================== OFFICER STATUS ====================
    private void updateOfficerStatus(Officer officer) {
        if (officer == null) return;

        Officer managed = getManagedOfficer(officer);
        long active = complaintRepository.countByAssignedOfficerAndStatusIn(
                managed, List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS));

        managed.setStatus(active == 0 ? OfficerStatus.AVAILABLE : OfficerStatus.BUSY);
        officerRepository.save(managed);
        log.info("Officer {} status updated to {}", managed.getId(), managed.getStatus());
    }

    // ==================== NOTIFICATIONS ====================
    private void notifyAdmins(Complaint c, String msg) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/admin/complaints",
                    new NotificationDto(c.getId(), msg)
            );
        } catch (Exception e) {
            log.warn("Failed to notify admins for complaint {}: {}", c.getId(), e.getMessage());
        }
    }

    private void notifyCitizen(Complaint c, String msg) {
        try {
            messagingTemplate.convertAndSendToUser(
                    c.getCitizen().getEmail().toLowerCase(),
                    "/queue/notify",
                    new NotificationDto(c.getId(), msg)
            );
        } catch (Exception e) {
            log.warn("Failed to notify citizen for complaint {}: {}", c.getId(), e.getMessage());
        }
    }

    // ==================== WORKLOAD (Single query) ====================
    public List<OfficerWorkloadResponse> getAllOfficersWorkload() {
        // ✅ Use the single-query method from OfficerRepository
        return officerRepository.fetchAllOfficersWorkload(
                List.of(ComplaintStatus.PENDING, ComplaintStatus.IN_PROGRESS)
        );
    }
}