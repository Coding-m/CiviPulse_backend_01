package com.example.demo.repositories;



import com.example.demo.entity.OfficerUpdateRequest;
import com.example.demo.entity.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficerUpdateRequestRepository
        extends JpaRepository<OfficerUpdateRequest, Long> {

    List<OfficerUpdateRequest> findByStatus(RequestStatus status);
}