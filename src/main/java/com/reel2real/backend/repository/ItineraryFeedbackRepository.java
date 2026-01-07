package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryFeedbackRepository
        extends JpaRepository<ItineraryFeedback, UUID> {

    // 👉 EXACT MATCH TO YOUR ENTITY
    boolean existsByTripIdAndDayNumberAndFeedbackType(
            UUID tripId,
            Integer dayNumber,
            String feedbackType
    );

    List<ItineraryFeedback> findByTripIdAndFeedbackType(
            UUID tripId,
            String feedbackType
    );

    List<ItineraryFeedback> findByTripId(UUID tripId);
}
