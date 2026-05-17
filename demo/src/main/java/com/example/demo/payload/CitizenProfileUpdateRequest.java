package com.example.demo.payload;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CitizenProfileUpdateRequest {

    private String name;
    private String phoneNo;
    private String address;
    private int age;
}
