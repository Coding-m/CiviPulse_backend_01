package com.example.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entity.Citizen;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {

    // For login
    Citizen findByEmail(String email);
    
 // For reset password
    Citizen findByResetToken(String resetToken);
}
