package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Reel;
import com.reel2real.backend.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReelRepository extends JpaRepository<Reel, UUID> {

    List<Reel> findByTrip(Trip trip);
}
