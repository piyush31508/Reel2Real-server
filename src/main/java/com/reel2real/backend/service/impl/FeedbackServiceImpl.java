package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.feedback.FeedbackRequest;
import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final ItineraryFeedbackRepository repository;

    @Override
    public void submitFeedback(FeedbackRequest request) {

        ItineraryFeedback feedback = new ItineraryFeedback();
        feedback.setTripId(request.getTripId());
        feedback.setDayNumber(request.getDayNumber());
        feedback.setPlaceId(request.getPlaceId());
        feedback.setFeedbackType(request.getFeedbackType());
        feedback.setReason(request.getReason());

        repository.save(feedback);
    }

}
