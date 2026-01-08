package com.reel2real.backend.controller;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
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

    // =====================================================
    //  DISLIKE PLACE
    // =====================================================
    @PostMapping("/dislike-place")
    public ResponseEntity<Void> dislikePlace(
            @RequestBody @Valid FeedbackRequest request
    ) {

        boolean already =
                feedbackRepository.existsByTripIdAndDayNumberAndFeedbackType(
                        request.getTripId(),
                        request.getDayNumber(),
                        "DISLIKE"
                );

        if (!already) {
            ItineraryFeedback feedback =
                    buildFeedback(request, true);

            feedbackRepository.save(feedback);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        }

        return ResponseEntity.ok().build();
    }

    // =====================================================
    //  DISLIKE WHOLE DAY
    // =====================================================
    @PostMapping("/dislike-day")
    public ResponseEntity<Void> dislikeDay(
            @RequestBody @Valid FeedbackRequest request
    ) {

        ItineraryFeedback feedback =
                buildFeedback(request, false);

        // whole day dislike → placeId null
        feedback.setPlaceId(null);

        feedbackRepository.save(feedback);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // =====================================================
    //  GET FEEDBACK OF TRIP
    // =====================================================
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryFeedback>> get(
            @PathVariable UUID tripId
    ) {

        List<ItineraryFeedback> list =
                feedbackRepository.findByTripId(tripId);

        return ResponseEntity.ok(list);
    }

    // =====================================================
    //  RESET FEEDBACK OF TRIP
    // =====================================================
    @DeleteMapping("/reset/{tripId}")
    public ResponseEntity<Void> reset(
            @PathVariable UUID tripId
    ) {

        feedbackRepository.deleteByTripId(tripId);

        return ResponseEntity.ok().build();
    }

    // =====================================================
    //  HELPER – COMMON BUILDER
    // =====================================================
    private ItineraryFeedback buildFeedback(
            FeedbackRequest request,
            boolean isPlaceDislike
    ) {
        return ItineraryFeedback.builder()
                .tripId(request.getTripId())
                .dayNumber(request.getDayNumber())
                .placeId(
                        isPlaceDislike
                                ? request.getPlaceId()
                                : null
                )
                .feedbackType("DISLIKE")
                .reason(request.getReason())
                .build();
    }
}
