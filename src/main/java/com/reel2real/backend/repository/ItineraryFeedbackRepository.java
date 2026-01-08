package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItineraryFeedbackRepository
        extends JpaRepository<ItineraryFeedback, UUID> {

    // ⭐ property names EXACT entity se match
    boolean existsByTripIdAndDayNumberAndFeedbackType(
            UUID tripId,
            Integer dayNumber,
            String feedbackType
    );

    List<ItineraryFeedback> findByTripId(UUID tripId);

    void deleteByTripId(UUID tripId);
}
