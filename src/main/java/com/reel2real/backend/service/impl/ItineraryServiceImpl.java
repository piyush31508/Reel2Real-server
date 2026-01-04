package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.entity.TripPlace;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.integration.NominatimClient;
import com.reel2real.backend.integration.OpenRouteServiceClient;
import com.reel2real.backend.integration.WeatherClient;
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

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final OpenRouteServiceClient orsClient;
    private final NominatimClient nominatimClient;
    private final PlaceRepository placeRepository;
    private final WeatherClient weatherClient;
    private final BudgetRealityService budgetRealityService;

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

        // 1️⃣ Load places + geo enrichment
        List<Place> places = tripPlaces.stream()
                .map(TripPlace::getPlace)
                .map(this::ensureLatLng)
                .toList();

        // 2️⃣ Preference-based ordering
        List<Place> preferenceSorted =
                sortByPreference(places, trip.getTravelStyle());

        // 3️⃣ Distance optimization
        List<Place> orderedPlaces =
                orderPlacesByDistance(preferenceSorted);

        // 4️⃣ Budget Reality Meter (ONCE)
        BudgetRealityResponse budgetReality =
                budgetRealityService.calculate(orderedPlaces, trip);

        // 5️⃣ Distribute places across days
        List<List<Place>> dayClusters =
                distributePlacesSmartly(orderedPlaces, trip.getTotalDays());

        // 6️⃣ Fetch weather ONCE
        WeatherType weather =
                WeatherType.from(
                        weatherClient.getWeather(trip.getDestinationCity())
                );

        // 7️⃣ Apply weather rules + route optimization per day
        for (int day = 0; day < dayClusters.size(); day++) {

            List<Place> weatherAdjusted =
                    applyWeatherRules(dayClusters.get(day), weather);

            dayClusters.set(day, optimizeDayRoute(weatherAdjusted));
        }

        // 8️⃣ Build final response
        List<ItineraryResponse> response = new ArrayList<>();

        for (int i = 0; i < dayClusters.size(); i++) {

            String reason = buildReasonMessage(
                    weather,
                    trip.getTravelStyle(),
                    dayClusters.get(i)
            );

            ItineraryResponse.ItineraryResponseBuilder builder =
                    ItineraryResponse.builder()
                            .dayNumber(i + 1)
                            .places(
                                    dayClusters.get(i)
                                            .stream()
                                            .map(Place::getName)
                                            .toList()
                            )
                            .reason(reason);

            // 🔥 Budget only on Day 1
            if (i == 0) {
                builder.budgetReality(budgetReality);
            }

            response.add(builder.build());
        }

        return response;
    }

    // =======================
    // STEP A: PREFERENCE SORT
    // =======================

    private List<Place> sortByPreference(List<Place> places, String travelStyle) {
        return places.stream()
                .sorted(Comparator.comparingInt(
                        p -> getPreferenceScore(p, travelStyle)
                ))
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

    // =======================
    // GEO ENRICHMENT
    // =======================

    private Place ensureLatLng(Place place) {

        if (place.getLatitude() != null && place.getLongitude() != null) {
            return place;
        }

        double[] latLng = nominatimClient.getLatLng(
                place.getName(),
                place.getCity(),
                place.getCountry()
        );

        if (latLng == null) return place;

        place.setLatitude(latLng[0]);
        place.setLongitude(latLng[1]);
        return placeRepository.save(place);
    }

    // =======================
    // STEP B: DISTANCE SORT
    // =======================

    private List<Place> orderPlacesByDistance(List<Place> places) {

        List<Place> geoReady = places.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();

        List<Place> noGeo = places.stream()
                .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
                .toList();

        if (geoReady.size() <= 1) {
            List<Place> result = new ArrayList<>(geoReady);
            result.addAll(noGeo);
            return result;
        }

        List<double[]> coords = geoReady.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .toList();

        double[][] matrix = orsClient.getDistanceMatrix(coords);

        boolean[] visited = new boolean[geoReady.size()];
        List<Place> ordered = new ArrayList<>();

        int current = 0;
        visited[current] = true;
        ordered.add(geoReady.get(current));

        for (int step = 1; step < geoReady.size(); step++) {

            double min = Double.MAX_VALUE;
            int next = -1;

            for (int i = 0; i < geoReady.size(); i++) {
                if (!visited[i] && matrix[current][i] < min) {
                    min = matrix[current][i];
                    next = i;
                }
            }

            visited[next] = true;
            ordered.add(geoReady.get(next));
            current = next;
        }

        ordered.addAll(noGeo);
        return ordered;
    }

    // =======================
    // STEP C: DAY DISTRIBUTION
    // =======================

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

    // =======================
    // STEP D: INTRA-DAY ROUTE
    // =======================

    private List<Place> optimizeDayRoute(List<Place> dayPlaces) {

        List<Place> geoReady = dayPlaces.stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .toList();

        List<Place> noGeo = dayPlaces.stream()
                .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
                .toList();

        if (geoReady.size() <= 2) {
            List<Place> result = new ArrayList<>(geoReady);
            result.addAll(noGeo);
            return result;
        }

        List<double[]> coords = geoReady.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .toList();

        double[][] matrix = orsClient.getDistanceMatrix(coords);

        boolean[] visited = new boolean[geoReady.size()];
        List<Place> ordered = new ArrayList<>();

        int current = 0;
        visited[current] = true;
        ordered.add(geoReady.get(current));

        for (int step = 1; step < geoReady.size(); step++) {

            double min = Double.MAX_VALUE;
            int next = -1;

            for (int i = 0; i < geoReady.size(); i++) {
                if (!visited[i] && matrix[current][i] < min) {
                    min = matrix[current][i];
                    next = i;
                }
            }

            visited[next] = true;
            ordered.add(geoReady.get(next));
            current = next;
        }

        ordered.addAll(noGeo);
        return ordered;
    }

    // =======================
    // STEP E: WEATHER RULES
    // =======================

    private List<Place> applyWeatherRules(
            List<Place> places,
            WeatherType weather
    ) {
        return places.stream()
                .sorted(Comparator.comparingInt(p -> weatherScore(p, weather)))
                .toList();
    }

    private int weatherScore(Place place, WeatherType weather) {

        String category =
                place.getCategory() != null
                        ? place.getCategory().toLowerCase()
                        : "";

        if (weather == WeatherType.RAIN) {
            return switch (category) {
                case "beach", "nature" -> 5;
                case "market", "food" -> 1;
                case "monument" -> 3;
                default -> 4;
            };
        }

        return switch (category) {
            case "beach", "nature" -> 1;
            case "monument" -> 2;
            case "market" -> 3;
            case "food" -> 4;
            default -> 5;
        };
    }

    // =======================
    // REASON MESSAGE
    // =======================

    private String buildReasonMessage(
            WeatherType weather,
            String travelStyle,
            List<Place> dayPlaces
    ) {
        List<String> reasons = new ArrayList<>();

        if (weather == WeatherType.RAIN) {
            reasons.add("adjusted plans due to expected rain");
        } else {
            reasons.add("optimized for clear weather conditions");
        }

        if ("relaxed".equalsIgnoreCase(travelStyle)) {
            reasons.add("kept the day light and relaxed");
        } else if ("packed".equalsIgnoreCase(travelStyle)) {
            reasons.add("packed more activities for maximum exploration");
        }

        boolean hasOutdoor =
                dayPlaces.stream()
                        .anyMatch(p ->
                                p.getCategory() != null &&
                                        List.of("beach", "nature")
                                                .contains(p.getCategory().toLowerCase())
                        );

        if (hasOutdoor && weather != WeatherType.RAIN) {
            reasons.add("included outdoor scenic locations");
        }

        return "This plan was generated because we " +
                String.join(", ", reasons) + ".";
    }
}