package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admins")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;
    
    @Column
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiry;


    @Enumerated(EnumType.STRING)
    private Role role; // Always Role.ADMIN
}