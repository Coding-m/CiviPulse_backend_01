package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.FeedbackViewResponse;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.OfficerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfficerFeedbackService {

    private final ComplaintRepository complaintRepository;
    private final OfficerRepository officerRepository;

    // ✅ Email passed in from controller (via @AuthenticationPrincipal or JWT filter)
    // instead of accessing SecurityContextHolder directly in the service
    public List<FeedbackViewResponse> getMyComplaintFeedbacks(String email) {
        Officer officer = officerRepository.findByEmail(email);
        if (officer == null) {
            throw new ResourceNotFoundException("Officer not found with email: " + email);
        }

        log.info("Fetching feedbacks for officer: {}", email);

        return complaintRepository.findByAssignedOfficer(officer)
                .stream()
                .filter(c -> c.getFeedback() != null)
                .map(this::mapToResponse)
                .toList();
    }

    // ==================== MAPPER ====================
    private FeedbackViewResponse mapToResponse(Complaint c) {
        Feedback f = c.getFeedback();
        Citizen citizen = c.getCitizen();

        return FeedbackViewResponse.builder()
                .complaintId(c.getId())
                .complaintTitle(c.getTitle())
                .complaintCategory(c.getCategory() != null ? c.getCategory().name() : null)
                .complaintStatus(c.getStatus() != null ? c.getStatus().name() : null) // ✅ null-safe
                .citizenId(citizen.getId())
                .citizenName(citizen.getName())
                .citizenLocation(citizen.getAddress())
                .rating(f.getRating())
                .officerBehaviourRating(f.getOfficerBehaviourRating())
                .resolutionStatus(f.getResolutionStatus())
                .timeliness(f.getTimeliness())
                .feedbackComment(f.getFeedbackComment())
                .reopened(f.getReopened())
                .submittedAt(f.getFeedbackSubmittedAt())
                .build();
    }
}