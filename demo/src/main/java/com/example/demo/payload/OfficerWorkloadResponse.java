package com.example.demo.payload;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OfficerWorkloadResponse {
    private Long id;
    private String name;
    private String email;
    private String department;
    private long activeComplaints;
    private String status; // AVAILABLE, BUSY, OVERLOADED
}
