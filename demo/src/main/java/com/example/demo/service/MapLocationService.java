package com.example.demo.service;



import com.example.demo.entity.MapLocation;
import com.example.demo.exception.BadRequestException;
import com.example.demo.payload.MapLocationRequest;
import com.example.demo.repositories.MapLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapLocationService {

    private final MapLocationRepository mapLocationRepository;

    public MapLocation saveLocation(MapLocationRequest request) {
        // ✅ Validate required fields
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException("Latitude and longitude are required");
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new BadRequestException("Latitude must be between -90 and 90");
        }
        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new BadRequestException("Longitude must be between -180 and 180");
        }

        MapLocation location = new MapLocation();
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setCitizenId(request.getCitizenId());
        location.setComplaintId(request.getComplaintId());

        MapLocation saved = mapLocationRepository.save(location);
        log.info("Map location saved with id: {} for complaint: {}",
                saved.getId(), request.getComplaintId());

        return saved;
    }
}