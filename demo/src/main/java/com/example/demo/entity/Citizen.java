package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "citizen")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;   // used for login

    private String phoneNo;

    private String address;

    private int age;

    private String password;  // will be encrypted
    
 // ✅ Optional fields for forgot password
    private String resetToken;  
    private LocalDateTime resetTokenExpiry;
    
    @Enumerated(EnumType.STRING)
    private Role role;  // <-- Add this field
}
