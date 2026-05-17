package com.example.demo.payload;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class OfficerProfileUpdateRequest {

    private String name;
    private String phoneNo;
    private String address;
    private int age;

}