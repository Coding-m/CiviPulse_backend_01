package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.ComplaintRequestDTO;
import com.example.demo.payload.NotificationDto;
import com.example.demo.payload.OfficerComplaintResponse;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.MapLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final MapLocationRepository mapLocationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Cloudinary cloudinary; // ✅ Injected via CloudinaryConfig

    // ====================== CREATE ======================
    public Complaint createFromDto(
            ComplaintRequestDTO dto,
            Citizen citizen,
            MultipartFile image
    ) {
        if (dto.getCitizenName() == null || dto.getCitizenPhone() == null) {
            throw new BadRequestException("Citizen name and phone number must be provided");
        }

        Complaint complaint = new Complaint();
        complaint.setTitle(dto.getTitle());
        complaint.setDescription(dto.getDescription());

        if (dto.getCategory() != null) {
            try {
                complaint.setCategory(ComplaintCategory.valueOf(dto.getCategory()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid category: " + dto.getCategory());
            }
        }

        complaint.setLocation(dto.getLocation());
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setCitizen(citizen);
        complaint.setCitizenName(dto.getCitizenName());
        complaint.setCitizenPhone(dto.getCitizenPhone());
        complaint.setShowCitizenInfoToAdmin(
                dto.getShowCitizenInfoToAdmin() == null || dto.getShowCitizenInfoToAdmin()
        );

        complaint.setPriority(Priority.MEDIUM);
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setComplaintStage(ComplaintStage.REGISTERED);

        // ✅ Upload to Cloudinary instead of local disk
        if (image != null && !image.isEmpty()) {
            complaint.setImageUrl(uploadImage(image));
        }

        Complaint saved = complaintRepository.save(complaint);
        saveMapLocation(saved);
        sendNotification(citizen.getEmail(), saved.getId(), saved.getStatus().name());

        return saved;
    }

    // ====================== UPDATE ======================
    public Complaint updateFromDto(
            Long citizenId,
            Long complaintId,
            ComplaintRequestDTO dto,
            MultipartFile image
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new BadRequestException("Unauthorized");
        }

        if (dto.getTitle() != null)       complaint.setTitle(dto.getTitle());
        if (dto.getDescription() != null) complaint.setDescription(dto.getDescription());
        if (dto.getCategory() != null) {
            try {
                complaint.setCategory(ComplaintCategory.valueOf(dto.getCategory()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid category: " + dto.getCategory());
            }
        }
        if (dto.getLocation() != null)   complaint.setLocation(dto.getLocation());
        if (dto.getLatitude() != null)   complaint.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null)  complaint.setLongitude(dto.getLongitude());
        if (dto.getCitizenName() != null) complaint.setCitizenName(dto.getCitizenName());
        if (dto.getCitizenPhone() != null) complaint.setCitizenPhone(dto.getCitizenPhone());
        if (dto.getShowCitizenInfoToAdmin() != null) {
            complaint.setShowCitizenInfoToAdmin(dto.getShowCitizenInfoToAdmin());
        }
        if (dto.getStatus() != null) {
            try {
                complaint.setStatus(ComplaintStatus.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + dto.getStatus());
            }
        }

        // ✅ Replace image on Cloudinary — delete old, upload new
        if (image != null && !image.isEmpty()) {
            deleteImageIfExists(complaint.getImageUrl());
            complaint.setImageUrl(uploadImage(image));
        }

        Complaint saved = complaintRepository.save(complaint);
        saveMapLocation(saved);
        sendNotification(saved.getCitizen().getEmail(), saved.getId(),
                "Updated: " + saved.getStatus().name());

        return saved;
    }

    // ====================== DELETE ======================
    public void deleteComplaint(Long citizenId, Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new BadRequestException("Unauthorized");
        }

        // ✅ Delete image from Cloudinary
        deleteImageIfExists(complaint.getImageUrl());
        complaintRepository.delete(complaint);
        sendNotification(complaint.getCitizen().getEmail(), complaintId, "Deleted");
    }

    // ====================== READ ======================
    public List<Complaint> getComplaintsByCitizen(Citizen citizen) {
        return complaintRepository.findByCitizen(citizen);
    }

    public Complaint getComplaintById(Long complaintId, Citizen citizen) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        if (citizen != null && !complaint.getCitizen().getId().equals(citizen.getId())) {
            throw new BadRequestException("Unauthorized");
        }

        return complaint;
    }

    public Complaint updateComplaintStage(Long complaintId, ComplaintStage stage) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
        complaint.setComplaintStage(stage);
        return complaintRepository.save(complaint);
    }

    // ====================== CLOUDINARY UPLOAD ======================
    private String uploadImage(MultipartFile image) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                image.getBytes(),
                ObjectUtils.asMap(
                    "folder", "smartcity/complaints",  // organized in Cloudinary dashboard
                    "resource_type", "image"
                )
            );
            String url = (String) uploadResult.get("secure_url");
            log.info("Image uploaded to Cloudinary: {}", url);
            return url; // ✅ Permanent HTTPS URL stored in DB
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    // ====================== CLOUDINARY DELETE ======================
    private void deleteImageIfExists(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        try {
            // Extract public_id from Cloudinary URL
            // URL format: https://res.cloudinary.com/<cloud>/image/upload/v123/<folder>/<filename>
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted image from Cloudinary: {}", publicId);
            }
        } catch (IOException e) {
            // Non-fatal — log and continue, don't fail the main operation
            log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
        }
    }

    private String extractPublicId(String imageUrl) {
        try {
            // Extract "smartcity/complaints/filename" from full Cloudinary URL
            String marker = "/upload/";
            int idx = imageUrl.indexOf(marker);
            if (idx == -1) return null;
            String afterUpload = imageUrl.substring(idx + marker.length());
            // Remove version prefix like "v1234567890/"
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }
            // Remove file extension
            int dotIdx = afterUpload.lastIndexOf(".");
            return dotIdx != -1 ? afterUpload.substring(0, dotIdx) : afterUpload;
        } catch (Exception e) {
            log.warn("Could not extract Cloudinary public_id from URL: {}", imageUrl);
            return null;
        }
    }

    // ====================== MAP LOCATION ======================
    private void saveMapLocation(Complaint complaint) {
        MapLocation map = new MapLocation();
        map.setLatitude(complaint.getLatitude());
        map.setLongitude(complaint.getLongitude());
        map.setCitizenId(complaint.getCitizen().getId());
        map.setComplaintId(complaint.getId());
        mapLocationRepository.save(map);
    }

    // ====================== WEBSOCKET ======================
    private void sendNotification(String email, Long complaintId, String status) {
        try {
            NotificationDto payload = new NotificationDto(complaintId, status);
            messagingTemplate.convertAndSendToUser(
                    email.toLowerCase(), "/queue/notify", payload);
            messagingTemplate.convertAndSend("/topic/admin/complaints", payload);
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}", email, e.getMessage());
        }
    }

    // ====================== MAPPER ======================
    public OfficerComplaintResponse mapToOfficerResponse(Complaint c) {
        String locationText = (c.getLocation() == null || c.getLocation().isBlank())
                ? "Coordinates: " + c.getLatitude() + ", " + c.getLongitude()
                : c.getLocation();

        Officer assignedOfficer = c.getAssignedOfficer();

        return OfficerComplaintResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .category(c.getCategory() != null ? c.getCategory().name() : null)
                .priority(c.getPriority() != null ? c.getPriority().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .description(c.getDescription())
                .location(locationText)
                .latitude(c.getLatitude())
                .longitude(c.getLongitude())
                .submissionDate(c.getSubmissionDate())
                .imageUrl(c.getImageUrl())
                .officerEvidenceUrl(c.getOfficerEvidenceUrl())
                .resolutionDate(c.getResolutionDate())
                .complaintStage(c.getComplaintStage() != null ? c.getComplaintStage().name() : null)
                .assignedOfficerName(assignedOfficer != null ? assignedOfficer.getName() : null)
                .assignedOfficerStatus(
                        assignedOfficer != null && assignedOfficer.getStatus() != null
                                ? assignedOfficer.getStatus().name() : null)
                .assignedOfficerDepartment(
                        assignedOfficer != null && assignedOfficer.getDepartment() != null
                                ? assignedOfficer.getDepartment().name() : null)
                .assignedOfficerActiveComplaints(
                        assignedOfficer != null ? assignedOfficer.getActiveComplaints() : 0L)
                .officerRemark(c.getOfficerRemark())
                .assignedDate(c.getAssignedDate())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .build();
    }
}