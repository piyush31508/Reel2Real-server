package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.integration.OpenRouteServiceClient;
import com.reel2real.backend.repository.TripPlaceRepository;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final OpenRouteServiceClient orsClient;

    @Override
    public List<ItineraryResponse> generateItinerary(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        List<TripPlace> tripPlaces = tripPlaceRepository.findByTrip(trip);

        if (tripPlaces.isEmpty()) {
            throw new ResourceNotFoundException("No places added to trip");
        }

        List<Place> places = tripPlaces.stream()
                .map(TripPlace::getPlace)
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();

        List<Place> orderedPlaces = orderPlacesByRoadDistance(places);

        return distributeIntoDays(
                orderedPlaces,
                trip.getTotalDays(),
                getPlacesPerDay(trip.getTravelStyle())
        );
    }

    /**
     * Uses ORS distance matrix + nearest-neighbor heuristic
     */
    private List<Place> orderPlacesByRoadDistance(List<Place> places) {

        List<double[]> coordinates = places.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .toList();

        double[][] distanceMatrix = orsClient.getDistanceMatrix(coordinates);

        List<Place> ordered = new ArrayList<>();
        boolean[] visited = new boolean[places.size()];

        int currentIndex = 0;
        ordered.add(places.get(currentIndex));
        visited[currentIndex] = true;

        for (int step = 1; step < places.size(); step++) {

            double minDistance = Double.MAX_VALUE;
            int nearestIndex = -1;

            for (int i = 0; i < places.size(); i++) {
                if (!visited[i] && distanceMatrix[currentIndex][i] < minDistance) {
                    minDistance = distanceMatrix[currentIndex][i];
                    nearestIndex = i;
                }
            }

            visited[nearestIndex] = true;
            ordered.add(places.get(nearestIndex));
            currentIndex = nearestIndex;
        }

        return ordered;
    }

    private List<ItineraryResponse> distributeIntoDays(
            List<Place> places,
            int totalDays,
            int placesPerDay
    ) {

        List<ItineraryResponse> itinerary = new ArrayList<>();
        int index = 0;

        for (int day = 1; day <= totalDays; day++) {

            List<String> dayPlaces = new ArrayList<>();

            for (int i = 0; i < placesPerDay && index < places.size(); i++) {
                dayPlaces.add(places.get(index).getName());
                index++;
            }

            itinerary.add(
                    ItineraryResponse.builder()
                            .dayNumber(day)
                            .places(dayPlaces)
                            .build()
            );
        }

        return itinerary;
    }

    private int getPlacesPerDay(String travelStyle) {
        if (travelStyle == null) return 3;

        return switch (travelStyle.toLowerCase()) {
            case "relaxed" -> 2;
            case "packed" -> 4;
            default -> 3;
        };
    }
}
