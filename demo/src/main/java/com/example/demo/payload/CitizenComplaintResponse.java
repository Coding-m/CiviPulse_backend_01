package com.example.demo.payload;



import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CitizenComplaintResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String complaintStage;
    private String officerRemark;
    private String adminRemark;
    private String officerEvidenceUrl;
    //private LocalDateTime expectedCompletionDate;
    private LocalDate expectedCompletionDate;
    private LocalDateTime submissionDate;
    private boolean deleted;
    private String deletionReason;
    private String clarificationMessage;

}
