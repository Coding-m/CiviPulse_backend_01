package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.payload.ComplaintRequestDTO;
import com.example.demo.payload.NotificationDto;
import com.example.demo.payload.OfficerComplaintResponse;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.MapLocationRepository;

import com.cloudinary.Cloudinary;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final MapLocationRepository mapLocationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final Cloudinary cloudinary;

    // ================= CLOUDINARY IMAGE UPLOAD =================
    private String uploadImageToCloudinary(MultipartFile image) {
        try {
            Map uploadResult = cloudinary.uploader().upload(
                    image.getBytes(),
                    Map.of(
                            "folder", "complaints",
                            "resource_type", "image",
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            );

            return uploadResult.get("secure_url").toString();

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    // ================= CREATE =================
    public Complaint createFromDto(
            ComplaintRequestDTO dto,
            Citizen citizen,
            MultipartFile image
    ) {

        Complaint complaint = new Complaint();

        complaint.setTitle(dto.getTitle());
        complaint.setDescription(dto.getDescription());

        if (dto.getCategory() != null) {
            complaint.setCategory(ComplaintCategory.valueOf(dto.getCategory()));
        }

        complaint.setLocation(dto.getLocation());
        complaint.setLatitude(dto.getLatitude());
        complaint.setLongitude(dto.getLongitude());
        complaint.setCitizen(citizen);

        complaint.setCitizenName(dto.getCitizenName());
        complaint.setCitizenPhone(dto.getCitizenPhone());

        complaint.setShowCitizenInfoToAdmin(true);
        complaint.setPriority(Priority.MEDIUM);
        complaint.setStatus(ComplaintStatus.PENDING);
        complaint.setComplaintStage(ComplaintStage.REGISTERED);

        // ================= IMAGE =================
        if (image != null && !image.isEmpty()) {
            complaint.setImageUrl(uploadImageToCloudinary(image));
        }

        Complaint saved = complaintRepository.save(complaint);

        saveMapLocation(saved);

        sendNotification(
                citizen.getEmail(),
                saved.getId(),
                saved.getStatus().name()
        );

        return saved;
    }

    // ================= UPDATE =================
    public Complaint updateFromDto(
            Long citizenId,
            Long complaintId,
            ComplaintRequestDTO dto,
            MultipartFile image
    ) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (dto.getTitle() != null) complaint.setTitle(dto.getTitle());
        if (dto.getDescription() != null) complaint.setDescription(dto.getDescription());

        if (dto.getCategory() != null) {
            complaint.setCategory(ComplaintCategory.valueOf(dto.getCategory()));
        }

        if (dto.getLocation() != null) complaint.setLocation(dto.getLocation());
        if (dto.getLatitude() != null) complaint.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) complaint.setLongitude(dto.getLongitude());
        if (dto.getCitizenName() != null) complaint.setCitizenName(dto.getCitizenName());
        if (dto.getCitizenPhone() != null) complaint.setCitizenPhone(dto.getCitizenPhone());

        if (dto.getStatus() != null) {
            complaint.setStatus(ComplaintStatus.valueOf(dto.getStatus()));
        }

        complaint.setShowCitizenInfoToAdmin(true);

        // ================= IMAGE UPDATE =================
        if (image != null && !image.isEmpty()) {
            complaint.setImageUrl(uploadImageToCloudinary(image));
        }

        Complaint saved = complaintRepository.save(complaint);

        saveMapLocation(saved);

        sendNotification(
                saved.getCitizen().getEmail(),
                saved.getId(),
                "Updated: " + saved.getStatus().name()
        );

        return saved;
    }

    // ================= DELETE =================
    public void deleteComplaint(Long citizenId, Long complaintId) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (!complaint.getCitizen().getId().equals(citizenId)) {
            throw new RuntimeException("Unauthorized");
        }

        complaintRepository.delete(complaint);

        sendNotification(
                complaint.getCitizen().getEmail(),
                complaintId,
                "Deleted"
        );
    }

    // ================= READ =================
    public Page<Complaint> getComplaintsByCitizen(Citizen citizen, Pageable pageable) {
        return complaintRepository.findByCitizen(citizen, pageable);
    }

    public Complaint getComplaintById(Long complaintId, Citizen citizen) {

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));

        if (citizen != null &&
                !complaint.getCitizen().getId().equals(citizen.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        return complaint;
    }

    // ================= MAP LOCATION =================
    private void saveMapLocation(Complaint complaint) {
        MapLocation map = new MapLocation();
        map.setLatitude(complaint.getLatitude());
        map.setLongitude(complaint.getLongitude());
        map.setCitizenId(complaint.getCitizen().getId());
        map.setComplaintId(complaint.getId());
        mapLocationRepository.save(map);
    }

    // ================= NOTIFICATION =================
    private void sendNotification(String email, Long complaintId, String status) {
        NotificationDto payload = new NotificationDto(complaintId, status);

        messagingTemplate.convertAndSendToUser(
                email.toLowerCase(),
                "/queue/notify",
                payload
        );

        messagingTemplate.convertAndSend(
                "/topic/admin/complaints",
                payload
        );
    }

    // ================= MAPPER =================
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
                                ? assignedOfficer.getStatus().name()
                                : null
                )
                .assignedOfficerDepartment(
                        assignedOfficer != null && assignedOfficer.getDepartment() != null
                                ? assignedOfficer.getDepartment().name()
                                : null
                )
                .assignedOfficerActiveComplaints(
                        assignedOfficer != null ? assignedOfficer.getActiveComplaints() : 0L
                )
                .officerRemark(c.getOfficerRemark())
                .assignedDate(c.getAssignedDate())
                .expectedCompletionDate(c.getExpectedCompletionDate())
                .build();
    }
}
