package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItineraryVersionRepository
        extends JpaRepository<ItineraryVersion, UUID> {

    List<ItineraryVersion> findByTripIdOrderByVersionNumberDesc(UUID tripId);
}
