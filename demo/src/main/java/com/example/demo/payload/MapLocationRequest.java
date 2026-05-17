package com.example.demo.payload;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapLocationRequest {

    private Double latitude;    // ✅ Double wrapper — supports null check
    private Double longitude;   // ✅ Double wrapper — supports null check
    private Long citizenId;
    private Long complaintId;
}