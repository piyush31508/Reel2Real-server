package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItineraryFeedbackRepository
        extends JpaRepository<ItineraryFeedback, UUID> {

    boolean existsByItineraryVersionIdAndDayNumberAndFeedbackType(
            UUID itineraryVersionId,
            Integer dayNumber,
            String feedbackType
    );

    List<ItineraryFeedback> findByItineraryVersionId(UUID itineraryVersionId);

    void deleteByItineraryVersionId(UUID itineraryVersionId);
}
