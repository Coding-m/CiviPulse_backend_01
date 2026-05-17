package com.example.demo.payload;

import com.example.demo.entity.ComplaintCategory;
import com.example.demo.entity.OfficerStatus;
import lombok.Getter;

@Getter
public class OfficerWorkloadResponse {

    private Long id;
    private String name;
    private String email;
    private String department;
    private String status;
    private long activeComplaints;

    // ✅ Constructor for JPQL — accepts enums, converts to String
    public OfficerWorkloadResponse(Long id, String name, String email,
                                   ComplaintCategory department,
                                   OfficerStatus status,
                                   Long activeComplaints) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department != null ? department.name() : null;
        this.status = status != null ? status.name() : null;
        this.activeComplaints = activeComplaints != null ? activeComplaints : 0L;
    }

    // ✅ Constructor for manual building (used in service with builder pattern)
    public OfficerWorkloadResponse(Long id, String name, String email,
                                   String department, String status,
                                   long activeComplaints) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.status = status;
        this.activeComplaints = activeComplaints;
    }
}
