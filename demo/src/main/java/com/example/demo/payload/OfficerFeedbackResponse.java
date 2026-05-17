package com.example.demo.payload;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OfficerFeedbackResponse {
     private Long complaintId;

    private Integer rating;
    private String resolutionStatus;
    private String timeliness;
    private Integer officerBehaviourRating;

    private String feedbackComment;
    private String feedbackImageUrl;
    private Boolean reopened;

    private LocalDateTime feedbackSubmittedAt;
}
