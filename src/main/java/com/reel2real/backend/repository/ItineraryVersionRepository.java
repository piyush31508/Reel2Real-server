package com.reel2real.backend.repository;

import com.reel2real.backend.entity.ItineraryVersion;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryVersionRepository extends JpaRepository<ItineraryVersion, UUID> {

    List<ItineraryVersion> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    @Query("select max(v.versionNumber) from ItineraryVersion v where v.tripId = :tripId")
    Optional<Integer> findMaxVersion(UUID tripId);

    ItineraryVersion findTopByTripIdAndDayNumberOrderByVersionNumberDesc(
            UUID tripId,
            Integer dayNumber
    );
}
