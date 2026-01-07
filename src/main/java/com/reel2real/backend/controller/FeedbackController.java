package com.reel2real.backend.controller;

import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.entity.ItineraryFeedback;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final ItineraryFeedbackRepository feedbackRepository;

    @PostMapping("/dislike-place")
    public ResponseEntity<Void> dislikePlace(
            @RequestBody @Valid FeedbackRequest request
    ) {

        boolean already =
                itineraryFeedbackRepository.existsByTripIdAndDayNumberAndFeedbackType(
                        request.getTripId(),
                        request.getDayNumber(),
                        request.getFeedbackType().toUpperCase()
                );


        if(!already) {

            ItineraryFeedback f = new ItineraryFeedback();

            f.setTripId(request.getTripId());
            f.setDayNumber(request.getDayNumber());
            f.setPlaceId(request.getPlaceId());
            f.setFeedbackType(
                    request.getFeedbackType().toUpperCase()
            );
            f.setReason(request.getReason());

            feedbackRepository.save(f);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/dislike-day")
    public ResponseEntity<Void> dislikeDay(
            @RequestBody @Valid FeedbackRequest request
    ) {

        ItineraryFeedback f = new ItineraryFeedback();

        f.setTripId(request.getTripId());
        f.setDayNumber(request.getDayNumber());
        f.setPlaceId(null);
        f.setFeedbackType(
                request.getFeedbackType().toUpperCase()
        );
        f.setReason(request.getReason());

        feedbackRepository.save(f);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryFeedback>> get(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(
                feedbackRepository.findByTripId(tripId)
        );
    }
}
