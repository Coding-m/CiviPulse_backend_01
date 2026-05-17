package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    // -------------------- BROADCAST TO ALL --------------------
    public void sendNotification(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
            log.info("Broadcast notification sent: {}", message);
        } catch (Exception e) {
            log.warn("Failed to send broadcast notification: {}", e.getMessage());
        }
    }

    // -------------------- SEND TO SPECIFIC USER --------------------
    // ✅ Added — useful for targeted notifications (citizen/officer/admin)
    public void sendToUser(String email, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    email.toLowerCase(), destination, payload);
            log.info("Notification sent to user {}: {}", email, destination);
        } catch (Exception e) {
            log.warn("Failed to send notification to {}: {}", email, e.getMessage());
        }
    }
}