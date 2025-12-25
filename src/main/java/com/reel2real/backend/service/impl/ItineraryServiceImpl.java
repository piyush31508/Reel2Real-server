package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.integration.NominatimClient;
import com.reel2real.backend.integration.OpenRouteServiceClient;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.repository.TripPlaceRepository;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final OpenRouteServiceClient orsClient;
    private final NominatimClient nominatimClient;
    private final PlaceRepository placeRepository;

    // =======================
    // PUBLIC API
    // =======================

    @Override
    @Transactional
    public List<ItineraryResponse> generateItinerary(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        List<TripPlace> tripPlaces = tripPlaceRepository.findByTrip(trip);

        if (tripPlaces.isEmpty()) {
            throw new ResourceNotFoundException("No places added to trip");
        }

        // 1. Load places + ensure lat/lng
        List<Place> places = tripPlaces.stream()
                .map(TripPlace::getPlace)
                .map(this::ensureLatLng)
                .toList();

        // 2. Order places by preference & distance
        List<Place> preferenceSorted =
                sortByPreference(places, trip.getTravelStyle());

        List<Place> orderedPlaces =
                orderPlacesByDistance(preferenceSorted);

        // 3. Cluster by daily time budgets
        List<List<Place>> dayClusters =
                clusterPlacesByTime(orderedPlaces, trip.getTotalDays());

        // ============= STEP D ENHANCEMENTS =============

        // D.1 handle empty days
        dayClusters = fixEmptyDays(dayClusters);

        // D.2 optimize each day's route by ORS
        for (int d = 0; d < dayClusters.size(); d++) {
            dayClusters.set(d, optimizeDayRoute(dayClusters.get(d)));
        }

        // D.3 rebalance if a day exceeds 8 hours
        rebalanceOverflow(dayClusters);

        // ============= RETURN RESPONSE =============

        List<ItineraryResponse> response = new ArrayList<>();

        for (int i = 0; i < dayClusters.size(); i++) {
            response.add(
                    ItineraryResponse.builder()
                            .dayNumber(i + 1)
                            .places(
                                    dayClusters.get(i)
                                            .stream()
                                            .map(Place::getName)
                                            .toList()
                            )
                            .build()
            );
        }

        return response;
    }

    private List<Place> sortByPreference(List<Place> places, String travelStyle) {
        return places.stream()
                .sorted(
                        Comparator.comparingInt(
                                p -> getPreferenceScore(p, travelStyle)
                        )
                )
                .toList();
    }

    private int getPreferenceScore(Place place, String travelStyle) {

        String category = place.getCategory() != null
                ? place.getCategory().toLowerCase()
                : "default";

        if (travelStyle == null) {
            return 5;
        }

        return switch (travelStyle.toLowerCase()) {

            case "relaxed" -> switch (category) {
                case "beach" -> 1;
                case "nature" -> 2;
                case "market" -> 3;
                case "monument" -> 4;
                case "food" -> 5;
                default -> 6;
            };

            case "packed" -> switch (category) {
                case "monument" -> 1;
                case "market" -> 2;
                case "food" -> 3;
                case "beach" -> 4;
                default -> 5;
            };

            default -> 5;
        };
    }

    // =======================
    // GEO HELPERS
    // =======================

    private Place ensureLatLng(Place place) {

        if (place.getLatitude() != null && place.getLongitude() != null) {
            return place;
        }

        double[] latLng = nominatimClient.getLatLng(
                place.getName(),
                place.getCity()
        );

        // Minimal-safe behavior: skip if geocoding fails
        if (latLng == null) {
            return place;
        }

        place.setLatitude(latLng[0]);
        place.setLongitude(latLng[1]);

        return placeRepository.save(place);
    }

    // =======================
    // DISTANCE OPTIMIZATION
    // =======================

    private List<Place> orderPlacesByDistance(List<Place> places) {

        if (places.size() <= 1) {
            return places;
        }

        List<double[]> coordinates = places.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .toList();

        double[][] matrix = orsClient.getDistanceMatrix(coordinates);

        List<Place> ordered = new ArrayList<>();
        boolean[] visited = new boolean[places.size()];

        int currentIndex = 0;
        ordered.add(places.get(currentIndex));
        visited[currentIndex] = true;

        for (int step = 1; step < places.size(); step++) {

            double minDistance = Double.MAX_VALUE;
            int nearestIndex = -1;

            for (int i = 0; i < places.size(); i++) {
                if (!visited[i] && matrix[currentIndex][i] < minDistance) {
                    minDistance = matrix[currentIndex][i];
                    nearestIndex = i;
                }
            }

            visited[nearestIndex] = true;
            ordered.add(places.get(nearestIndex));
            currentIndex = nearestIndex;
        }

        return ordered;
    }

    // =======================
    // STEP C: TIME-AWARE CLUSTERING
    // =======================

    private List<List<Place>> clusterPlacesByTime(
            List<Place> orderedPlaces,
            int totalDays
    ) {

        double DAILY_HOURS = 8.0;

        List<List<Place>> result = new ArrayList<>();
        int index = 0;

        for (int day = 1; day <= totalDays; day++) {

            double remainingHours = DAILY_HOURS;
            List<Place> dayPlaces = new ArrayList<>();

            while (index < orderedPlaces.size()) {

                Place place = orderedPlaces.get(index);
                double required = estimateVisitHours(place);

                if (required > remainingHours) {
                    break;
                }

                dayPlaces.add(place);
                remainingHours -= required;
                index++;
            }

            result.add(dayPlaces);
        }

        return result;
    }

    private double estimateVisitHours(Place place) {

        if (place.getCategory() == null) {
            return 2.0;
        }

        return switch (place.getCategory().toLowerCase()) {
            case "beach" -> 2.5;
            case "monument" -> 2.0;
            case "market" -> 1.5;
            case "food" -> 1.0;
            default -> 2.0;
        };
    }

    // =======================
    // STEP D: FIX EMPTY DAYS
    // =======================

    private List<List<Place>> fixEmptyDays(List<List<Place>> clusters) {

        for (int i = 0; i < clusters.size(); i++) {
            if (clusters.get(i).isEmpty() && i > 0) {

                List<Place> previous = clusters.get(i - 1);

                if (!previous.isEmpty()) {
                    Place moved = previous.remove(previous.size() - 1);
                    clusters.get(i).add(moved);
                }
            }
        }
        return clusters;
    }

    // =======================
    // STEP D: INTRA-DAY OPTIMIZATION
    // =======================

    private List<Place> optimizeDayRoute(List<Place> dayPlaces) {

        if (dayPlaces.size() <= 2) return dayPlaces;

        List<double[]> coords = dayPlaces.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .toList();

        double[][] matrix = orsClient.getDistanceMatrix(coords);

        boolean[] visited = new boolean[dayPlaces.size()];
        List<Place> result = new ArrayList<>();

        int current = 0;
        visited[current] = true;
        result.add(dayPlaces.get(current));

        for (int step = 1; step < dayPlaces.size(); step++) {

            double best = Double.MAX_VALUE;
            int next = -1;

            for (int i = 0; i < dayPlaces.size(); i++) {
                if (!visited[i] && matrix[current][i] < best) {
                    best = matrix[current][i];
                    next = i;
                }
            }

            visited[next] = true;
            result.add(dayPlaces.get(next));
            current = next;
        }

        return result;
    }

    // =======================
    // STEP D: REBALANCE OVERFLOW
    // =======================

    private void rebalanceOverflow(List<List<Place>> clusters) {

        double DAILY = 8.0;

        for (int i = 0; i < clusters.size() - 1; i++) {

            List<Place> today = clusters.get(i);
            double hours = today.stream().mapToDouble(this::estimateVisitHours).sum();

            while (hours > DAILY && !today.isEmpty()) {

                Place moved = today.remove(today.size() - 1);

                clusters.get(i + 1).add(0, moved);

                hours = today.stream()
                        .mapToDouble(this::estimateVisitHours)
                        .sum();
            }
        }
    }
}
