package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryVersionDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItineraryVersionDayRepository
        extends JpaRepository<ItineraryVersionDay, UUID> {

    List<ItineraryVersionDay> findByItineraryVersionId(UUID versionId);
}
