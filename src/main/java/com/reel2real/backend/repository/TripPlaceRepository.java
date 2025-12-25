package com.reel2real.backend.repository;

import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TripPlaceRepository extends JpaRepository<TripPlace, UUID> {

    List<TripPlace> findByTrip(Trip trip);

    boolean existsByTripAndPlace(Trip trip, Place place);
}
