package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryVersionDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItineraryVersionDayRepository
        extends JpaRepository<ItineraryVersionDay, UUID> {

    Optional<ItineraryVersionDay>
    findByItineraryVersionIdAndDayNumber(
            UUID itineraryVersionId,
            int dayNumber
    );
    List<ItineraryVersionDay> findByItineraryVersionId(UUID versionId);
}
