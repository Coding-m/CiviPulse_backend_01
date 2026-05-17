package com.example.demo.service;

import com.example.demo.entity.Citizen;
import com.example.demo.entity.Complaint;
import com.example.demo.entity.ComplaintStatus;
import com.example.demo.entity.Feedback;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.payload.FeedbackRequest;
import com.example.demo.repositories.CitizenRepository;
import com.example.demo.repositories.ComplaintRepository;
import com.example.demo.repositories.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenFeedbackService {

    private final ComplaintRepository complaintRepository;
    private final CitizenRepository citizenRepository;
    private final FeedbackRepository feedbackRepository;

    // ✅ Email passed from controller — no SecurityContextHolder in service
    public Citizen getCitizenByEmail(String email) {
        Citizen citizen = citizenRepository.findByEmail(email);
        if (citizen == null) {
            throw new ResourceNotFoundException("Citizen not found with email: " + email);
        }
        return citizen;
    }

    // -------------------- SUBMIT FEEDBACK --------------------
    public String submitFeedback(String email, Long complaintId, FeedbackRequest request) {
        Citizen citizen = getCitizenByEmail(email);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        // ✅ Ownership check — citizen can only submit feedback on their own complaint
        if (!complaint.getCitizen().getId().equals(citizen.getId())) {
            throw new BadRequestException("You can only submit feedback on your own complaints");
        }

        // ✅ Prevent duplicate feedback
        if (complaint.getFeedback() != null) {
            throw new BadRequestException("Feedback already submitted for this complaint");
        }

        Feedback feedback = Feedback.builder()
                .rating(request.getRating())
                .resolutionStatus(request.getResolutionStatus())
                .timeliness(request.getTimeliness())
                .officerBehaviourRating(request.getOfficerBehaviourRating())
                .feedbackComment(request.getFeedbackComment())
                .reopened(request.getReopened())
                .feedbackSubmittedAt(LocalDateTime.now())
                .feedbackBy(citizen)
                .complaint(complaint)
                .build();

        feedbackRepository.save(feedback);
        log.info("Feedback submitted by citizen {} for complaint {}", email, complaintId);

        // ✅ Reopen complaint if requested
        if (Boolean.TRUE.equals(request.getReopened())) {
            complaint.setStatus(ComplaintStatus.REOPENED);
            complaintRepository.save(complaint);
            log.info("Complaint {} reopened by citizen {}", complaintId, email);
        }

        return "Feedback submitted successfully";
    }

    // -------------------- GET FEEDBACK FOR A COMPLAINT --------------------
    public Feedback getFeedbackByComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));

        Feedback feedback = complaint.getFeedback();
        if (feedback == null) {
            throw new ResourceNotFoundException("No feedback submitted for this complaint");
        }

        return feedback;
    }

    // -------------------- GET ALL FEEDBACK BY CITIZEN --------------------
    public List<Feedback> getFeedbackByCitizen(String email) {
        Citizen citizen = getCitizenByEmail(email);
        return feedbackRepository.findAllByFeedbackBy(citizen);
    }
}