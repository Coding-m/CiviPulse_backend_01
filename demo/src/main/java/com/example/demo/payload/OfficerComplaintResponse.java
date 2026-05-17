package com.example.demo.payload;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder  // ✅ builder added
public class OfficerComplaintResponse {

    private Long id;
    private String title;
    private String category;
    private String priority;
    private String status;
    private String officerRemark;
    private LocalDateTime assignedDate;
   // private LocalDateTime expectedCompletionDate;
    private LocalDate expectedCompletionDate;
    private LocalDateTime resolutionDate;
    private String location;
    private String description;
    private Double latitude;
    private Double longitude;
    private LocalDateTime submissionDate;
    private String imageUrl;
    private String officerEvidenceUrl;
    private String complaintStage;

    // Assigned Officer Info
    private String assignedOfficerName;
    private String assignedOfficerStatus;
    private String assignedOfficerDepartment;
    private Long assignedOfficerActiveComplaints;
}
