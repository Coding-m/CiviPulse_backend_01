package com.example.demo.repositories;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.entity.Officer;
import com.example.demo.entity.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ================== CITIZEN ==================

    List<Complaint> findByCitizen(Citizen citizen);

    List<Complaint> findByCitizenId(Long citizenId);

    // ✅ Pagination methods
    Page<Complaint> findByCitizen(Citizen citizen, Pageable pageable);

    Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);


    // ================== OFFICER ==================

    List<Complaint> findByAssignedOfficer(Officer officer);

    Page<Complaint> findByAssignedOfficer(Officer officer, Pageable pageable);

    long countByAssignedOfficerAndStatusIn(
            Officer officer,
            List<ComplaintStatus> statuses
    );

    @Query("""
        SELECT c FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
    """)
    List<Complaint> findByAssignedOfficerId(
            @Param("officerId") Long officerId
    );

    @Query("""
        SELECT c FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
    """)
    Page<Complaint> findByAssignedOfficerId(
            @Param("officerId") Long officerId,
            Pageable pageable
    );

    @Query("""
        SELECT c FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
        OR c.assignedOfficer IS NULL
    """)
    List<Complaint> findAllByAssignedOfficerIdOrUnassigned(
            @Param("officerId") Long officerId
    );


    // ================== ADMIN / FEEDBACK ==================

    List<Complaint> findByFeedbackIsNotNull();

    List<Complaint> findByAssignedOfficerIsNull();

    @Query("""
        SELECT COUNT(c)
        FROM Complaint c
        WHERE c.assignedOfficer.id = :officerId
        AND c.status <> 'RESOLVED'
    """)
    long countActiveComplaintsByOfficer(
            @Param("officerId") Long officerId
    );


    // ================== FILTER ==================

// @Query("""
//     SELECT c FROM Complaint c
//     WHERE c.deleted = false
//     AND (:status IS NULL OR c.status = :status)
//     AND (:priority IS NULL OR c.priority = :priority)
//     AND (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')))
// """)
// List<Complaint> findAllFiltered(
//         @Param("search") String search,
//         @Param("status") ComplaintStatus status,
//         @Param("priority") Priority priority
// );
// }
