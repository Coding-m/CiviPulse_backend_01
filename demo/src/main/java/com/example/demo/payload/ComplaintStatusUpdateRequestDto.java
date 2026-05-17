package com.example.demo.payload;

import lombok.Data;

@Data
public class ComplaintStatusUpdateRequestDto {
    private String status; // PENDING, IN_PROGRESS, RESOLVED, etc.
}
