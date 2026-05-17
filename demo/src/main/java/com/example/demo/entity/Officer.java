package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "officers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Officer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phoneNo;
    
    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private int age;
    

    // Officer Department (TRAFFIC, CRIME, CYBER, etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintCategory department;

    // Officer role (OFFICER / ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.OFFICER;

    // Officer availability status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OfficerStatus status = OfficerStatus.AVAILABLE;

    // Number of active complaints (calculated, not stored)
    @Transient
    private Long activeComplaints;

    // Safe getter (prevents NullPointerException)
    public Long getActiveComplaints() {
        return activeComplaints != null ? activeComplaints : 0L;
    }

    // ---------------- Password reset support ----------------
    private String resetToken;
    private LocalDateTime resetTokenExpiry;
}