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

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryResponse>> generate(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(
                itineraryService.generateItinerary(tripId)
        );
    }

//    @PostMapping("/trip/{tripId}/regenerate")
//    public ResponseEntity<List<ItineraryResponse>> regenerate(
//            @PathVariable UUID tripId
//    ) {
//        return ResponseEntity.ok(
//                itineraryService.regenerateItinerary(tripId)
//        );
//    }

}
