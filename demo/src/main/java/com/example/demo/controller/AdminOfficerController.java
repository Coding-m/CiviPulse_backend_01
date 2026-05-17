package com.example.demo.controller;

import com.example.demo.payload.OfficerWorkloadResponse;
import com.example.demo.service.AdminComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/officers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminOfficerController {

    private final AdminComplaintService adminComplaintService;

    @GetMapping("/workload-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OfficerWorkloadResponse> getOfficerWorkloadSummary() {
        return adminComplaintService.getAllOfficersWorkload();
    }
}
