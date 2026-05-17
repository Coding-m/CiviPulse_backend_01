package com.example.demo.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ComplainAPIResponse {
    private boolean success;
    private String message;
    private Object data; // can be Complaint, List<Complaint>, etc.
}
