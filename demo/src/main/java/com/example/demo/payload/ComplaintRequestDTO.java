package com.example.demo.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintRequestDTO {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    // 🔴 REQUIRED — prevents NULL category DB error
    @NotBlank(message = "Category is required")
    private String category; // maps to ComplaintCategory enum

    // 📝 Optional written location/address
    private String location;

<<<<<<< HEAD:demo/src/main/java/com/example/demo/payload/ComplaintRequestDTO.java
    @NotBlank(message = "Citizen phone is required")
    private String citizenPhone;

   

=======
  

    @NotBlank(message = "Citizen phone is required")
    private String citizenPhone;

>>>>>>> 52200fb (Fix ComplaintRepository pagination):src/main/java/com/example/demo/payload/ComplaintRequestDTO.java
    // 📌 optional status update
    private String status;

    // 🌍 REQUIRED — Leaflet coordinates
    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
