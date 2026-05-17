package com.example.demo.payload;

import lombok.Data;

@Data
public class FeedbackRequest {

    private Integer rating;               
    private String resolutionStatus;      
    private String timeliness;           
    private Integer officerBehaviourRating;
    private String feedbackComment;       
        
    private Boolean reopened;  
    private Long complaintId;             // link feedback to complaint
    private Long citizenId;               // who submitted
}
