package com.example.demo.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CitizenLoginResponse {
    private String message;
    private String token;
    private String role;
    
}
