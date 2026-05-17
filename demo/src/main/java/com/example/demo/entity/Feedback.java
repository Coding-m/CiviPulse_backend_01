package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;                // 1–5 stars
    private String resolutionStatus;       // RESOLVED, PARTIALLY_RESOLVED, NOT_RESOLVED
    private String timeliness;             // ON_TIME, SLIGHT_DELAY, VERY_LATE
    private Integer officerBehaviourRating;
    private String feedbackComment;
    private String feedbackImageUrl;
    private Boolean reopened;
    private LocalDateTime feedbackSubmittedAt;

    // Citizen who submitted feedback
    @ManyToOne
    @JoinColumn(name = "feedback_by_citizen_id")
    private Citizen feedbackBy;

    // Feedback belongs to which complaint
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;
}       