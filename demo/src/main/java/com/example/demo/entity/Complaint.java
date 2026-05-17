package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id")
    @JsonIgnoreProperties({"complaints", "hibernateLazyInitializer", "handler"})
    private Citizen citizen;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "officer_id", nullable = true)
    @JsonIgnoreProperties({"assignedComplaints", "hibernateLazyInitializer", "handler"})
    private Officer assignedOfficer;

    // Officer dashboard fields
    private LocalDateTime assignedDate;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = true)
    private String location;

    @Column(nullable = false)
    private String citizenName;

    @Column(nullable = false)
    private String citizenPhone;

    @Builder.Default
    private boolean showCitizenInfoToAdmin = true;

    @Column(length = 1000)
    private String officerRemark;

    @Column(length = 500)
    private String deletionReason;

    @Column(length = 500)
    private String clarificationMessage;

    @Column(name = "officer_evidence_url")
    private String officerEvidenceUrl;

    // Soft delete flag
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private LocalDateTime submissionDate = LocalDateTime.now();

    // ✅ Use LocalDate for expected completion
    private LocalDate expectedCompletionDate;

    private LocalDateTime resolutionDate;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin assignedAdmin;

    @Column(length = 1000)
    private String adminRemark;

    @Column
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ComplaintStage complaintStage = ComplaintStage.REGISTERED;
    
    
 
   


    @OneToOne(mappedBy = "complaint", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("complaint")
    private Feedback feedback;

	
}
