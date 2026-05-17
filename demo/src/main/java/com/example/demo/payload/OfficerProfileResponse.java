package com.example.demo.payload;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OfficerProfileResponse {
    
    private String name;
    private String email;
    private String phoneNo;
    private String address;
    private int age;

}
