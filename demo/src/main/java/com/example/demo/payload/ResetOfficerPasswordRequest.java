package com.example.demo.payload;



import lombok.Data;

@Data
public class ResetOfficerPasswordRequest {
    private String officerEmail;
    private String newPassword;
}
