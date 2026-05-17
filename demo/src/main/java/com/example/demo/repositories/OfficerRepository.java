package com.example.demo.repositories;

import com.example.demo.entity.Officer;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.payload.OfficerWorkloadResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficerRepository extends JpaRepository<Officer, Long> {

    // Find officer by email
    Officer findByEmail(String email);

    // ✅ NEW — Single query replaces N+1 pattern in AdminComplaintService.getAllOfficersWorkload()
    // Counts active (PENDING + IN_PROGRESS) complaints per officer via LEFT JOIN
    @Query("""
        SELECT new com.example.demo.payload.OfficerWorkloadResponse(
            o.id,
            o.name,
            o.email,
            CAST(o.department AS string),
            CAST(o.status AS string),
            COUNT(c)
        )
        FROM Officer o
        LEFT JOIN Complaint c
            ON c.assignedOfficer = o
            AND c.status IN :activeStatuses
        GROUP BY o.id, o.name, o.email, o.department, o.status
    """)
    List<OfficerWorkloadResponse> fetchAllOfficersWorkload(
        @Param("activeStatuses") List<ComplaintStatus> activeStatuses
    );
}