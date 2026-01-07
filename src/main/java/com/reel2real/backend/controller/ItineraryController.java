package com.reel2real.backend.controller;

import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;

    // =====================================================
    // PHASE 2 – Generate (SAME ENDPOINT)
    // =====================================================
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryResponse>> generateItinerary(
            @PathVariable UUID tripId
    ) {
        List<ItineraryResponse> result =
                itineraryService.generateItinerary(tripId);

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // PHASE 3 – Regenerate (SAME ENDPOINT)
    // =====================================================
    @PostMapping("/trip/{tripId}/regenerate")
    public ResponseEntity<List<ItineraryResponse>> regenerateItinerary(
            @PathVariable UUID tripId
    ) {
        List<ItineraryResponse> result =
                itineraryService.regenerateItinerary(tripId);

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // 🔥 PHASE 3 – NEW: Lock Day Endpoint
    // (ONLY ADDITION – prev code safe)
    // =====================================================
    @PostMapping("/trip/{tripId}/lock-day/{dayNumber}")
    public ResponseEntity<Void> lockDay(
            @PathVariable UUID tripId,
            @PathVariable int dayNumber
    ) {

        if (dayNumber <= 0) {
            throw new IllegalArgumentException("Day number must be ≥ 1");
        }

        itineraryService.lockDay(tripId, dayNumber);

        return ResponseEntity.ok().build();
    }
}
