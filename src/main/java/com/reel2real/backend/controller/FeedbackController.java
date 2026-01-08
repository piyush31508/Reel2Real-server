package com.reel2real.backend.controller;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.entity.ItineraryVersion;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.repository.ItineraryVersionRepository;
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

    @PostMapping("/dislike-place")
    public ResponseEntity<Void> dislikePlace(
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

        boolean already =
                feedbackRepository
                        .existsByItineraryVersionIdAndDayNumberAndFeedbackType(
                                version.getId(),
                                request.getDayNumber(),
                                "DISLIKE"
                        );

        if (!already) {
            feedbackRepository.save(
                    buildFeedback(request, true, version.getId())
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.ok().build();
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