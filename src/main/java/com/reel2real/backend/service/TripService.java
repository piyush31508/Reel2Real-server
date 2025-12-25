package com.reel2real.backend.service;

import com.reel2real.backend.dto.trip.TripCreateRequest;
import com.reel2real.backend.dto.trip.TripPlaceRequest;
import com.reel2real.backend.dto.trip.TripPlaceResponse;
import com.reel2real.backend.dto.trip.TripResponse;

import java.util.List;
import java.util.UUID;

public interface TripService {

    // EXISTING
    TripResponse createTrip(UUID userId, TripCreateRequest request);

    List<TripResponse> getTripsForUser(UUID userId);

    List<TripPlaceResponse> suggestPlaces(UUID tripId);

    // 🔥 NEW — TripPlace FEATURES
    void addPlaceToTrip(UUID tripId, TripPlaceRequest request);

    List<TripPlaceResponse> getTripPlaces(UUID tripId);
}
