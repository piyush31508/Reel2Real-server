package com.reel2real.backend.service;

import com.reel2real.backend.dto.trip.TripCreateRequest;
import com.reel2real.backend.dto.trip.TripResponse;

import java.util.List;
import java.util.UUID;

public interface TripService {

    TripResponse createTrip(UUID userId, TripCreateRequest request);

    List<TripResponse> getTripsForUser(UUID userId);
}
