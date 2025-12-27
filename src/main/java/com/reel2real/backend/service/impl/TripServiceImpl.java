package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.trip.TripCreateRequest;
import com.reel2real.backend.dto.trip.TripPlaceRequest;
import com.reel2real.backend.dto.trip.TripPlaceResponse;
import com.reel2real.backend.dto.trip.TripResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import com.reel2real.backend.entity.User;
import com.reel2real.backend.exception.BadRequestException;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.repository.TripPlaceRepository;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.repository.UserRepository;
import com.reel2real.backend.service.TripService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    // 🔥 NEW
    private final TripPlaceRepository tripPlaceRepository;
    private final PlaceRepository placeRepository;

    // =========================
    // EXISTING TRIP LOGIC
    // =========================

    @Override
    public TripResponse createTrip(UUID userId, TripCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Trip trip = Trip.builder()
                .user(user)
                .destinationCity(request.getDestinationCity())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalDays(request.getTotalDays())
                .travelStyle(request.getTravelStyle())
                .createdAt(LocalDateTime.now())
                .build();

        Trip savedTrip = tripRepository.save(trip);
        return mapToResponse(savedTrip);
    }

    @Override
    public List<TripResponse> getTripsForUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return tripRepository.findByUser(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TripPlaceResponse> suggestPlaces(UUID tripId) {
        return List.of();
    }

    // =========================
    // 🔥 TRIPPLACE LOGIC
    // =========================

    @Override
    public void addPlaceToTrip(UUID tripId, TripPlaceRequest request) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        Place place = placeRepository.findById(request.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Place not found"));

        if (tripPlaceRepository.existsByTripAndPlace(trip, place)) {
            throw new BadRequestException("Place already added to trip");
        }

        TripPlace tripPlace = TripPlace.builder()
                .trip(trip)
                .place(place)
                .priority(request.getPriority())
                .build();

        tripPlaceRepository.save(tripPlace);
    }

    @Override
    public List<TripPlaceResponse> getTripPlaces(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        return tripPlaceRepository.findByTrip(trip)
                .stream()
                .map(tp -> TripPlaceResponse.builder()
                        .id(tp.getId())
                        .placeId(tp.getPlace().getId())
                        .placeName(tp.getPlace().getName())
                        .city(tp.getPlace().getCity())
                        .priority(tp.getPriority())
                        .build())
                .toList();
    }

    // =========================
    // MAPPER
    // =========================

    private TripResponse mapToResponse(Trip trip) {
        return TripResponse.builder()
                .id(trip.getId())
                .destinationCity(trip.getDestinationCity())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalDays(trip.getTotalDays())
                .travelStyle(trip.getTravelStyle())
                .build();
    }
}
