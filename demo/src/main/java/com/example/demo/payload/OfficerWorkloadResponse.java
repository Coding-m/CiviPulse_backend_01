package com.example.demo.payload;

import com.example.demo.entity.ComplaintCategory;
import com.example.demo.entity.OfficerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OfficerWorkloadResponse {

    private Long id;
    private String name;
    private String email;
    private String department;
    private String status;
    private long activeComplaints;

    // ✅ Constructor accepts enum types — converts to String internally
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
}
