package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.*;
import com.example.demo.repositories.OfficerRepository;
import com.example.demo.repositories.OfficerUpdateRequestRepository;
import com.example.demo.security.JwtUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficerService {

    private final OfficerRepository officerRepository;
    private final OfficerUpdateRequestRepository updateRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    // ================= OFFICER LOGIN =================
    public LoginResponse login(LoginRequest request) {
        Officer officer = officerRepository.findByEmail(request.getEmail());

        if (officer == null || !passwordEncoder.matches(request.getPassword(), officer.getPassword())) {
            // ✅ Throw exception — controller returns proper HTTP 401
            throw new BadRequestException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(officer.getEmail(), officer.getRole().name());
        log.info("Officer logged in: {}", request.getEmail());

        return new LoginResponse("Login successful", token, officer.getRole().name());
    }

    // ================= REQUEST PROFILE UPDATE =================
    public String requestProfileUpdate(Officer officer, OfficerProfileUpdateRequest request) {
        try {
            String jsonData = objectMapper.writeValueAsString(request);

            OfficerUpdateRequest updateRequest = OfficerUpdateRequest.builder()
                    .officer(officer)
                    .requestedData(jsonData)
                    .status(RequestStatus.PENDING)
                    .requestedAt(LocalDateTime.now())
                    .build();

            updateRequestRepository.save(updateRequest);
            log.info("Profile update request submitted for officer: {}", officer.getEmail());

            return "Profile update request sent for admin approval";

        } catch (JsonProcessingException e) {
            // ✅ Specific exception instead of generic catch-all
            log.error("Failed to serialize profile update request for officer {}: {}",
                    officer.getEmail(), e.getMessage());
            throw new RuntimeException("Failed to submit update request");
        }
    }

    // ================= GET OFFICER BY EMAIL =================
    // ✅ Useful helper for controllers/filters instead of returning null
    public Officer getOfficerByEmail(String email) {
        Officer officer = officerRepository.findByEmail(email);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found with email: " + email);
        }
        return officer;
    }
}