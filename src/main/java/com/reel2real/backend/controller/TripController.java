package com.reel2real.backend.controller;

import com.reel2real.backend.dto.trip.TripCreateRequest;
import com.reel2real.backend.dto.trip.TripPlaceRequest;
import com.reel2real.backend.dto.trip.TripPlaceResponse;
import com.reel2real.backend.dto.trip.TripResponse;
import com.reel2real.backend.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    // =========================
    // EXISTING
    // =========================

    @PostMapping("/user/{userId}")
    public ResponseEntity<TripResponse> createTrip(
            @PathVariable UUID userId,
            @Valid @RequestBody TripCreateRequest request
    ) {
        return new ResponseEntity<>(
                tripService.createTrip(userId, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TripResponse>> getTrips(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                tripService.getTripsForUser(userId)
        );
    }

    // =========================
    // 🔥 TRIPPLACE APIs
    // =========================

    @PostMapping("/{tripId}/places")
    public ResponseEntity<Void> addPlaceToTrip(
            @PathVariable UUID tripId,
            @Valid @RequestBody TripPlaceRequest request
    ) {
        tripService.addPlaceToTrip(tripId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{tripId}/suggestions")
    public ResponseEntity<List<TripPlaceResponse>> suggestions(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(tripService.suggestPlaces(tripId));
    }

    @GetMapping("/{tripId}/places")
    public ResponseEntity<List<TripPlaceResponse>> getTripPlaces(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(
                tripService.getTripPlaces(tripId)
        );
    }
}
