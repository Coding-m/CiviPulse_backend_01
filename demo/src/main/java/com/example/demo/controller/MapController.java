package com.example.demo.controller;

import com.example.demo.entity.MapLocation;
import com.example.demo.payload.MapLocationRequest;
import com.example.demo.service.MapLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MapController {

    private final MapLocationService mapLocationService;

    @PostMapping("/save-location")
    public ResponseEntity<MapLocation> saveLocation(
            @RequestBody MapLocationRequest request) {
        MapLocation saved = mapLocationService.saveLocation(request);
        return ResponseEntity.ok(saved);
    }
}