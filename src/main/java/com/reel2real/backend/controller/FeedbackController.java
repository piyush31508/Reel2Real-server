package com.reel2real.backend.controller;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.entity.ItineraryVersion;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.repository.ItineraryVersionRepository;
import com.reel2real.backend.repository.TripRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final ItineraryFeedbackRepository feedbackRepository;
    private final ItineraryVersionRepository versionRepository;
    private final TripRepository tripRepository;

    @PostMapping("/dislike-place")
    public ResponseEntity<Void> dislikePlace(@RequestBody @Valid FeedbackRequest request) {
        // 1. Get the latest version
        ItineraryVersion version = versionRepository
                .findTopByTripIdOrderByCreatedAtDesc(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("No itinerary found"));

        // 2. FETCH THE TRIP (Missing in your current code)
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // 3. Save with the Trip object
        feedbackRepository.save(
                ItineraryFeedback.builder()
                        .itineraryVersionId(version.getId())
                        .dayNumber(request.getDayNumber())
                        .placeId(request.getPlaceId())
                        .feedbackType("DISLIKE")
                        .reason(request.getReason())
                        .trip(trip) // <--- CRITICAL FIX
                        .build()
        );
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/dislike-day")
    public ResponseEntity<Void> dislikeDay(
            @RequestBody @Valid FeedbackRequest request
    ) {

        ItineraryVersion version =
                versionRepository
                        .findTopByTripIdOrderByCreatedAtDesc(
                                request.getTripId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No itinerary found for this trip. Please generate an itinerary first."
                                )
                        );

        feedbackRepository.save(
                buildFeedback(request, false, version.getId())
        );

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryFeedback>> get(
            @PathVariable UUID tripId
    ) {

        ItineraryVersion version =
                versionRepository
                        .findTopByTripIdOrderByCreatedAtDesc(tripId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No itinerary found for this trip"
                                )
                        );

        return ResponseEntity.ok(
                feedbackRepository.findByItineraryVersionId(version.getId())
        );
    }

    @DeleteMapping("/reset/{tripId}")
    public ResponseEntity<Void> reset(
            @PathVariable UUID tripId
    ) {

        ItineraryVersion version =
                versionRepository
                        .findTopByTripIdOrderByCreatedAtDesc(tripId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "No itinerary found for this trip"
                                )
                        );

        feedbackRepository.deleteByItineraryVersionId(version.getId());
        return ResponseEntity.ok().build();
    }

    private ItineraryFeedback buildFeedback(
            FeedbackRequest request,
            boolean isPlaceDislike,
            UUID versionId
    ) {
        return ItineraryFeedback.builder()
                .itineraryVersionId(versionId)
                .dayNumber(request.getDayNumber())
                .placeId(isPlaceDislike ? request.getPlaceId() : null)
                .feedbackType("DISLIKE")
                .reason(request.getReason())
                .build();
    }
}