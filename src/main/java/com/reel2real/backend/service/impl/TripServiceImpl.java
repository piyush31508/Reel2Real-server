package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.trip.TripCreateRequest;
import com.reel2real.backend.dto.trip.TripResponse;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.User;
import com.reel2real.backend.exception.BadRequestException;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.repository.UserRepository;
import com.reel2real.backend.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;

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
