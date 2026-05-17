package com.example.demo.repositories;

import com.example.demo.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // Find admin by email
    Admin findByEmail(String email);

    // Find admin by reset token (for OTP reset)
    Admin findByResetToken(String resetToken);
}
