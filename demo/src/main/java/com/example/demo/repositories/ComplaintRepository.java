package com.example.demo.repositories;

import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.entity.Officer;
import com.example.demo.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ------------------ CITIZEN ------------------
    List<Complaint> findByCitizen(Citizen citizen);
    List<Complaint> findByCitizenId(Long citizenId);

    // ------------------ OFFICER ------------------
    List<Complaint> findByAssignedOfficer(Officer officer);
    long countByAssignedOfficerAndStatusIn(Officer officer, List<ComplaintStatus> statuses);

    @Query("""
        SELECT c FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
        OR c.assignedOfficer IS NULL
    """)
    List<Complaint> findAllByAssignedOfficerIdOrUnassigned(@Param("officerId") Long officerId);

    // ------------------ ADMIN / FEEDBACK ------------------
    List<Complaint> findByFeedbackIsNotNull();
    List<Complaint> findByAssignedOfficerIsNull();

    @Query("""
        SELECT COUNT(c) FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
        AND c.status <> 'RESOLVED'
    """)
    long countActiveComplaintsByOfficer(@Param("officerId") Long officerId);

    // ✅ Single findAllFiltered — PostgreSQL compatible, no CAST needed
    @Query("""
        SELECT c FROM Complaint c
        WHERE c.deleted = false
        AND (:status IS NULL OR UPPER(c.status) = UPPER(:status))
        AND (:priority IS NULL OR UPPER(c.priority) = UPPER(:priority))
        AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))
    """)
    List<Complaint> findAllFiltered(
        @Param("search") String search,
        @Param("status") String status,
        @Param("priority") String priority
    );
}