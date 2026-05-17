package com.example.demo.payload;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FeedbackViewResponse {

    // Complaint
    private Long complaintId;
    private String complaintTitle;
    private String complaintCategory;
    private String complaintStatus;

    // Citizen
    private Long citizenId;
    private String citizenName;
    private String citizenLocation;

    // Feedback
    private Integer rating;
    private Integer officerBehaviourRating;
    private String resolutionStatus;
    private String timeliness;
    private String feedbackComment;
    private Boolean reopened;
    private LocalDateTime submittedAt;
}
