package com.example.demo.payload;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String resetToken;
    private String newPassword;
}
