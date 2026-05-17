package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ✅ Reads from application.properties / Render env variable
    @Value("${spring.mail.username}")
    private String fromEmail;

    // ====================== OTP EMAIL ======================
    @Async  // ✅ Non-blocking — doesn't hold up the HTTP request
    public void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        log.info("Sending OTP email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);   // ✅ Explicit sender
            helper.setTo(toEmail);
            helper.setSubject("Your OTP for Password Reset - CivicPulse Hub");
            helper.setText(buildOtpEmailHtml(otp), true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    // ====================== CREDENTIALS EMAIL ======================
    @Async  // ✅ Non-blocking
    public void sendCredentialsEmail(String toEmail, String password, String userType)
            throws MessagingException {
        log.info("Sending credentials email to: {} ({})", toEmail, userType);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);   // ✅ Explicit sender
            helper.setTo(toEmail);
            helper.setSubject("Your " + userType + " Account Credentials - CivicPulse Hub");
            helper.setText(buildCredentialsEmailHtml(toEmail, password, userType), true);

            mailSender.send(message);
            log.info("Credentials email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send credentials email to {}: {}", toEmail, e.getMessage());
            throw e;
        }
    }

    // ====================== HTML BUILDERS ======================
    // ✅ Separate methods — clean, readable, easy to edit later

    private String buildOtpEmailHtml(String otp) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                .container { font-family: Arial, sans-serif; padding: 20px;
                             max-width: 500px; margin: auto;
                             border: 1px solid #ddd; border-radius: 10px; }
                .header    { text-align: center; }
                .otp       { font-size: 28px; font-weight: bold;
                             color: #2c7be5; padding: 10px 0; letter-spacing: 4px; }
                .footer    { margin-top: 20px; font-size: 12px;
                             color: #555; text-align: center; }
              </style>
            </head>
            <body>
              <div class='container'>
                <div class='header'><h2>CivicPulse Hub</h2></div>
                <p>Hello,</p>
                <p>Your OTP for password reset is:</p>
                <div class='otp'>%s</div>
                <p>This OTP is valid for <strong>10 minutes</strong>.</p>
                <p>If you didn't request this, please ignore this email.</p>
                <div class='footer'>© 2025 CivicPulse Hub. All rights reserved.</div>
              </div>
            </body>
            </html>
            """.formatted(otp);
    }

    private String buildCredentialsEmailHtml(String toEmail, String password, String userType) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                .container   { font-family: Arial, sans-serif; padding: 20px;
                               max-width: 500px; margin: auto;
                               border: 1px solid #ddd; border-radius: 10px; }
                .header      { text-align: center; }
                .credentials { font-size: 16px; background: #f4f4f4;
                               padding: 12px; border-radius: 6px;
                               margin: 10px 0; }
                .label       { color: #555; font-size: 13px; }
                .value       { font-weight: bold; color: #2c7be5; }
                .footer      { margin-top: 20px; font-size: 12px;
                               color: #555; text-align: center; }
              </style>
            </head>
            <body>
              <div class='container'>
                <div class='header'><h2>CivicPulse Hub</h2></div>
                <p>Hello <strong>%s</strong>,</p>
                <p>Your account has been created. Use the credentials below to log in:</p>
                <div class='credentials'>
                  <div><span class='label'>Email: </span>
                       <span class='value'>%s</span></div>
                  <div><span class='label'>Password: </span>
                       <span class='value'>%s</span></div>
                </div>
                <p>Please <strong>change your password</strong> after your first login.</p>
                <div class='footer'>© 2025 CivicPulse Hub. All rights reserved.</div>
              </div>
            </body>
            </html>
            """.formatted(userType, toEmail, password);
    }
}