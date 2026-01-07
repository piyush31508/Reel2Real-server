package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.ItineraryFeedback;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.integration.NominatimClient;
import com.reel2real.backend.integration.OpenRouteServiceClient;
import com.reel2real.backend.integration.WeatherClient;
import com.reel2real.backend.itinerary.ConfidenceCalculator;
import com.reel2real.backend.repository.ItineraryFeedbackRepository;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.repository.TripPlaceRepository;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.service.BudgetRealityService;
import com.reel2real.backend.service.ItineraryService;
import com.reel2real.backend.weather.WeatherType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final PlaceRepository placeRepository;
    private final WeatherClient weatherClient;
    private final BudgetRealityService budgetRealityService;
    private final ItineraryFeedbackRepository feedbackRepository;
    private final OpenRouteServiceClient orsClient;
    private final NominatimClient nominatimClient;

    // =====================================================
    // PHASE 2 – GENERATE ITINERARY (PREVIOUS CODE)
    // =====================================================

    @Override
    @Transactional
    public List<ItineraryResponse> generateItinerary(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        List<TripPlace> tripPlaces = tripPlaceRepository.findByTrip(trip);
        if (tripPlaces.isEmpty()) {
            throw new ResourceNotFoundException("No places added to trip");
        }

        // 1️⃣ Load & geo-enrich
        List<Place> places = tripPlaces.stream()
                .map(TripPlace::getPlace)
                .map(this::ensureLatLng)
                .toList();

        // 2️⃣ Preference + distance optimization
        List<Place> orderedPlaces =
                orderPlacesByDistance(
                        sortByPreference(places, trip.getTravelStyle())
                );

        // 3️⃣ Budget Reality (ONCE)
        BudgetRealityResponse budgetReality =
                budgetRealityService.calculate(orderedPlaces, trip);

        // 4️⃣ Distribute across days
        List<List<Place>> dayClusters =
                distributePlacesSmartly(orderedPlaces, trip.getTotalDays());

        // 5️⃣ Weather (ONCE)
        WeatherType weather =
                WeatherType.from(
                        weatherClient.getWeather(trip.getDestinationCity())
                );

        // 6️⃣ Build Response
        List<ItineraryResponse> response = new ArrayList<>();

        for (int i = 0; i < dayClusters.size(); i++) {

            List<Place> optimizedDay =
                    optimizeDayRoute(
                            applyWeatherRules(dayClusters.get(i), weather)
                    );

            String reason =
                    buildReasonMessage(weather, trip.getTravelStyle(), optimizedDay);

            int confidence =
                    ConfidenceCalculator.calculate(
                            optimizedDay,
                            weather,
                            trip.getTravelStyle(),
                            i == 0 && budgetReality != null
                    );

            ItineraryResponse.ItineraryResponseBuilder builder =
                    ItineraryResponse.builder()
                            .dayNumber(i + 1)
                            .places(
                                    optimizedDay.stream()
                                            .map(Place::getName)
                                            .toList()
                            )
                            .reason(reason)
                            .confidenceScore(confidence)
                            .confidenceLabel(
                                    ConfidenceCalculator.label(confidence)
                            );

            // 🔥 Budget shown only on Day 1
            if (i == 0) {
                builder.budgetReality(budgetReality);
            }

            response.add(builder.build());
        }

        return response;
    }

    // =====================================================
    // PHASE 3 – NEW METHODS
    // =====================================================

    @Override
    @Transactional
    public void lockDay(UUID tripId, int dayNumber) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        trip.getLockedDays().add(dayNumber);

        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public List<ItineraryResponse> regenerateItinerary(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found"));

        // ----- PLACES FETCH (PREVIOUS) -----
        List<Place> allPlaces = tripPlaceRepository.findByTrip(trip)
                .stream()
                .map(TripPlace::getPlace)
                .toList();

        // ----- DISLIKED PLACES -----
        Set<UUID> dislikedPlaces =
                feedbackRepository.findByTripIdAndFeedbackType(
                                tripId, "DISLIKE"
                        )
                        .stream()
                        .map(ItineraryFeedback::getPlaceId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        // ----- DISLIKED DAYS -----
        Set<Integer> dislikedDays =
                feedbackRepository.findByTripIdAndFeedbackType(
                                tripId, "DISLIKE"
                        )
                        .stream()
                        .filter(f -> f.getPlaceId() == null)
                        .map(ItineraryFeedback::getDayNumber)
                        .collect(Collectors.toSet());

        // ----- FILTER -----
        List<Place> filteredPlaces = allPlaces.stream()
                .filter(p -> !dislikedPlaces.contains(p.getId()))
                .toList();

        if (filteredPlaces.isEmpty()) {
            throw new IllegalStateException(
                    "All places were disliked. Cannot regenerate itinerary."
            );
        }

        // ----- PREVIOUS HELPERS REUSED -----
        List<Place> orderedPlaces =
                orderPlacesByDistance(
                        sortByPreference(
                                filteredPlaces,
                                trip.getTravelStyle()
                        )
                );

        // ----- REDISTRIBUTE -----
        List<List<Place>> dayClusters =
                distributePlacesSmartly(
                        orderedPlaces,
                        trip.getTotalDays()
                );

        WeatherType weather =
                WeatherType.from(
                        weatherClient.getWeather(
                                trip.getDestinationCity()
                        )
                );

        List<ItineraryResponse> response = new ArrayList<>();

        for (int i = 0; i < dayClusters.size(); i++) {

            int currentDay = i + 1;

            // 🔥 LOCKED DAYS RESPECT
            if(trip.getLockedDays().contains(currentDay))
                continue;

            if (dislikedDays.contains(currentDay))
                continue;

            List<Place> optimized =
                    ensureLatLngList(
                            optimizeDayRoute(
                                    dayClusters.get(i)
                            )
                    );

            response.add(
                    ItineraryResponse.builder()
                            .dayNumber(currentDay)
                            .places(
                                    optimized.stream()
                                            .map(Place::getName)
                                            .toList()
                            )
                            .reason("Regenerated based on feedback")
                            .confidenceScore(80)
                            .confidenceLabel("Good")
                            .build()
            );
        }

        return response;
    }


    // =====================================================
    // HELPERS (PREVIOUS — ALL PRESERVED)
    // =====================================================

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

        String category =
                place.getCategory() != null
                        ? place.getCategory().toLowerCase()
                        : "";

        if (travelStyle == null) return 5;

        return switch (travelStyle.toLowerCase()) {

            case "relaxed" -> switch (category) {
                case "beach" -> 1;
                case "nature" -> 2;
                case "market" -> 3;
                case "monument" -> 4;
                default -> 5;
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

    private Place ensureLatLng(Place place) {

        if (place.getLatitude() != null && place.getLongitude() != null) {
            return place;
        }

        double[] latLng =
                nominatimClient.getLatLng(
                        place.getName(),
                        place.getCity(),
                        place.getCountry()
                );

        if (latLng == null) return place;

        place.setLatitude(latLng[0]);
        place.setLongitude(latLng[1]);

        return placeRepository.save(place);
    }

    private List<Place> orderPlacesByDistance(List<Place> places) {

        List<Place> geoReady = places.stream()
                .filter(p ->
                        p.getLatitude() != null &&
                                p.getLongitude() != null
                )
                .toList();

        if (geoReady.size() <= 1) return places;

        List<double[]> coords = geoReady.stream()
                .map(p ->
                        new double[]{
                                p.getLongitude(),
                                p.getLatitude()
                        }
                )
                .toList();

        double[][] matrix =
                orsClient.getDistanceMatrix(coords);

        boolean[] visited =
                new boolean[geoReady.size()];

        List<Place> ordered =
                new ArrayList<>();

        int current = 0;
        visited[current] = true;
        ordered.add(geoReady.get(current));

        for (int step = 1; step < geoReady.size(); step++) {

            double min = Double.MAX_VALUE;
            int next = -1;

            for (int i = 0; i < geoReady.size(); i++) {

                if (!visited[i] &&
                        matrix[current][i] < min) {

                    min = matrix[current][i];
                    next = i;
                }
            }

            visited[next] = true;
            ordered.add(geoReady.get(next));
            current = next;
        }

        return ordered;
    }

    private List<List<Place>> distributePlacesSmartly(
            List<Place> places,
            int totalDays
    ) {

        List<List<Place>> result = new ArrayList<>();

        for (int i = 0; i < totalDays; i++) {
            result.add(new ArrayList<>());
        }

        for (int i = 0; i < places.size(); i++) {
            result.get(i % totalDays).add(places.get(i));
        }

        return result;
    }

    private List<Place> optimizeDayRoute(List<Place> dayPlaces) {
        return dayPlaces;
    }

    private List<Place> applyWeatherRules(
            List<Place> places,
            WeatherType weather
    ) {
        return places;
    }

    private String buildReasonMessage(
            WeatherType weather,
            String travelStyle,
            List<Place> dayPlaces
    ) {
        return "Generated using weather, distance, preferences, and real-world cost data.";
    }

    // =====================================================
    // ADDITIONAL HELPER – NON BREAKING
    // =====================================================

    private List<Place> ensureLatLngList(List<Place> list){
        return list.stream()
                .map(this::ensureLatLng)
                .toList();
    }
}
