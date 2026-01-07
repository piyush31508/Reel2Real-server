package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final TripRepository tripRepository;
    private final ItineraryFeedbackRepository feedbackRepository;

    @Override
    @Transactional
    public void submitFeedback(FeedbackRequest request) {

        // validate trip exists
        boolean tripExists =
                tripRepository.existsById(request.getTripId());

        if(!tripExists) {
            throw new ResourceNotFoundException("Trip not found");
        }

        ItineraryFeedback feedback = new ItineraryFeedback();

        feedback.setTripId(request.getTripId());
        feedback.setDayNumber(request.getDayNumber());
        feedback.setPlaceId(request.getPlaceId()); // nullable allowed
        feedback.setFeedbackType(
                request.getFeedbackType().toUpperCase()
        );
        feedback.setReason(request.getReason());

        feedbackRepository.save(feedback);
    }
}
