package com.reel2real.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.*;
import com.reel2real.backend.exception.ResourceNotFoundException;
import com.reel2real.backend.integration.NominatimClient;
import com.reel2real.backend.integration.OpenRouteServiceClient;
import com.reel2real.backend.integration.WeatherClient;
import com.reel2real.backend.repository.*;
import com.reel2real.backend.service.BudgetRealityService;
import com.reel2real.backend.service.ItineraryService;
import com.reel2real.backend.weather.WeatherType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryServiceImpl implements ItineraryService {

    private final TripRepository tripRepository;
    private final TripPlaceRepository tripPlaceRepository;
    private final PlaceRepository placeRepository;
    private final ItineraryFeedbackRepository feedbackRepository;
    private final ItineraryVersionRepository versionRepository;
    private final BudgetRealityService budgetRealityService;
    private final ItineraryVersionDayRepository versionDayRepository;
    private final WeatherClient weatherClient;

    private final OpenRouteServiceClient orsClient;
    private final NominatimClient nominatimClient;

    private final ObjectMapper mapper = new ObjectMapper();

    // =====================================================
    // PHASE 2 – GENERATE ITINERARY
    // =====================================================
    @Override
    @Transactional
    public List<ItineraryResponse> generateItinerary(UUID tripId) {

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip not found")
                );

        List<Place> places =
                tripPlaceRepository.findByTrip(trip)
                        .stream()
                        .map(TripPlace::getPlace)
                        .map(this::ensureLatLng)
                        .collect(Collectors.toList());

        if (places.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No places added to trip"
            );
        }

        List<Place> ordered =
                orderPlacesByDistance(
                        sortByPreference(
                                places,
                                trip.getTravelStyle()
                        )
                );

        BudgetRealityResponse reality =
                budgetRealityService.calculate(ordered, trip);

        WeatherType weather =
                WeatherType.from(
                        weatherClient.getWeather(
                                trip.getDestinationCity()
                        )
                );

        List<List<Place>> dayClusters =
                distributePlacesSmartly(
                        ordered,
                        trip.getTotalDays()
                );

        List<ItineraryResponse> response =
                new ArrayList<>();

        for (int i = 0;
             i < dayClusters.size();
             i++) {

            int currentDay =
                    i + 1;

            if (trip.getLockedDays()
                    .contains(currentDay))
                continue;

            List<String> names =
                    applyWeatherRules(
                            dayClusters.get(i),
                            weather
                    ).stream()
                            .map(Place::getName)
                            .toList();

            int confidence = 80;

            // 🔥 Save ORIGINAL version on day 1
            createVersion(
                    tripId,
                    currentDay,
                    names,
                    confidence,
                    "ORIGINAL"
            );

            ItineraryResponse r =
                    ItineraryResponse.builder()
                            .dayNumber(currentDay)
                            .places(names)
                            .reason("Initial itinerary")
                            .confidenceScore(confidence)
                            .confidenceLabel("Good")
                            .budgetReality(currentDay == 1 ? reality : null)
                            .build();


            response.add(r);
        }

        return response;
    }

    private List<Place> orderPlacesByDistance(List<Place> places) {

        if (places.size() <= 2) return places;

        // ORS expects [longitude, latitude]
        List<double[]> coordinates = places.stream()
                .map(p -> new double[]{
                        p.getLongitude(),
                        p.getLatitude()
                })
                .toList();

        double[][] matrix = orsClient.getDistanceMatrix(coordinates);

        int n = places.size();
        boolean[] visited = new boolean[n];
        List<Place> ordered = new ArrayList<>();

        int current = 0;
        visited[current] = true;
        ordered.add(places.get(current));

        for (int step = 1; step < n; step++) {

            int next = -1;
            double min = Double.MAX_VALUE;

            for (int i = 0; i < n; i++) {
                if (!visited[i] && matrix[current][i] < min) {
                    min = matrix[current][i];
                    next = i;
                }
            }

            visited[next] = true;
            ordered.add(places.get(next));
            current = next;
        }

        return ordered;
    }

    // =====================================================
    // PHASE 3 – LOCK DAY
    // =====================================================
    @Override
    @Transactional
    public void lockDay(UUID tripId, int dayNumber) {

        // 1️⃣ Get latest active version
        ItineraryVersion version =
                versionRepository
                        .findTopByTripIdOrderByCreatedAtDesc(tripId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("No itinerary version found")
                        );

        // 2️⃣ Fetch version-day
        ItineraryVersionDay day =
                versionDayRepository
                        .findByItineraryVersionIdAndDayNumber(
                                version.getId(),
                                dayNumber
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Day not found in version")
                        );

        // 3️⃣ Lock it
        day.setLocked(true);
        versionDayRepository.save(day);
    }



    // =====================================================
    // PHASE 3 – REGENERATE ITINERARY
    // =====================================================
    @Override
    @Transactional
    public List<ItineraryResponse> regenerateItinerary(UUID tripId) {

        Trip trip = loadTrip(tripId);

        ItineraryVersion sourceVersion = loadSourceVersion(tripId);

        FeedbackContext feedback =
                loadFeedbackForVersion(sourceVersion.getId());

        List<Place> candidatePlaces =
                loadAndFilterPlaces(trip, feedback.dislikedPlaces());

        List<List<Place>> clusters =
                buildClusters(candidatePlaces, trip);

        WeatherType weather =
                resolveWeather(trip);

        return generateRegeneratedResponse(
                tripId,
                sourceVersion,
                clusters,
                feedback,
                weather
        );
    }
    private Trip loadTrip(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Trip not found"));
    }

    private ItineraryVersion loadSourceVersion(UUID tripId) {
        return versionRepository
                .findTopByTripIdOrderByCreatedAtDesc(tripId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No itinerary version found"
                        ));
    }

    private record FeedbackContext(
            Set<UUID> dislikedPlaces,
            Set<Integer> dislikedDays
    ) {}

    private FeedbackContext loadFeedbackForVersion(UUID versionId) {

        List<ItineraryFeedback> feedback =
                feedbackRepository.findByItineraryVersionId(versionId);

        Set<UUID> places =
                feedback.stream()
                        .map(ItineraryFeedback::getPlaceId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Set<Integer> days =
                feedback.stream()
                        .filter(f -> f.getPlaceId() == null)
                        .map(ItineraryFeedback::getDayNumber)
                        .collect(Collectors.toSet());

        return new FeedbackContext(places, days);
    }

    private List<Place> loadAndFilterPlaces(
            Trip trip,
            Set<UUID> dislikedPlaces
    ) {

        List<Place> filtered =
                tripPlaceRepository.findByTrip(trip)
                        .stream()
                        .map(TripPlace::getPlace)
                        .filter(p -> !dislikedPlaces.contains(p.getId()))
                        .toList();

        if (filtered.isEmpty()) {
            throw new IllegalStateException(
                    "All places were disliked. Cannot regenerate."
            );
        }

        return filtered;
    }

    private List<List<Place>> buildClusters(
            List<Place> places,
            Trip trip
    ) {

        List<Place> ordered =
                orderPlacesByDistance(
                        sortByPreference(
                                places,
                                trip.getTravelStyle()
                        )
                );

        return distributePlacesSmartly(
                ordered,
                trip.getTotalDays()
        );
    }

    private WeatherType resolveWeather(Trip trip) {
        return WeatherType.from(
                weatherClient.getWeather(
                        trip.getDestinationCity()
                )
        );
    }

    private List<ItineraryResponse> generateRegeneratedResponse(
            UUID tripId,
            ItineraryVersion sourceVersion,
            List<List<Place>> clusters,
            FeedbackContext feedback,
            WeatherType weather
    ) {

        List<ItineraryResponse> response = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {

            int day = i + 1;

            if (isDayLocked(sourceVersion.getId(), day)) continue;
            if (feedback.dislikedDays().contains(day)) continue;

            List<String> names =
                    applyWeatherRules(
                            clusters.get(i),
                            weather
                    ).stream()
                            .map(Place::getName)
                            .toList();

            createVersion(
                    tripId,
                    day,
                    names,
                    80,
                    "REGENERATED"
            );

            response.add(
                    ItineraryResponse.builder()
                            .dayNumber(day)
                            .places(names)
                            .reason(buildReason(feedback))
                            .confidenceScore(80)
                            .confidenceLabel("Good")
                            .build()
            );
        }

        return response;
    }

    private boolean isDayLocked(UUID versionId, int day) {
        return versionDayRepository
                .findByItineraryVersionIdAndDayNumber(versionId, day)
                .map(ItineraryVersionDay::isLocked)
                .orElse(false);
    }

    private String buildReason(FeedbackContext feedback) {

        int p = feedback.dislikedPlaces().size();
        int d = feedback.dislikedDays().size();

        if (p == 0 && d == 0)
            return "Regenerated";

        return "Removed "
                + p + " disliked places"
                + (d > 0 ? " and skipped " + d + " days" : "");
    }



    // =====================================================
    // HELPERS
    // =====================================================

    private List<Place> sortByPreference(List<Place> places,
                                         String style) {

        if (style == null)
            return places;

        return places.stream()
                .sorted(
                        Comparator.comparingInt(
                                p ->
                                        resolvePreferenceScore(
                                                p.getCategory(),
                                                style
                                        )
                        )
                )
                .toList();
    }

    private int resolvePreferenceScore(String category,
                                       String style) {

        String c =
                category != null
                        ? category.toLowerCase()
                        : "";

        return switch (style.toLowerCase()) {

            case "relaxed" -> switch (c) {
                case "beach" -> 1;
                case "nature" -> 2;
                case "market" -> 3;
                case "food" -> 4;
                default -> 5;
            };

            case "packed" -> switch (c) {
                case "monument" -> 1;
                case "market" -> 2;
                case "food" -> 3;
                default -> 4;
            };

            default -> 5;
        };
    }

    private Place ensureLatLng(Place place) {

        if (place.getLatitude() != null &&
                place.getLongitude() != null) {
            return place;
        }

        double[] latLng =
                nominatimClient.getLatLng(
                        place.getName(),
                        place.getCity(),
                        place.getCountry()
                );

        if (latLng == null)
            return place;

        place.setLatitude(latLng[0]);
        place.setLongitude(latLng[1]);

        return placeRepository.save(place);
    }

    private List<List<Place>> distributePlacesSmartly(
            List<Place> places,
            int days
    ) {

        List<List<Place>> result =
                new ArrayList<>();

        for (int i = 0; i < days; i++)
            result.add(
                    new ArrayList<>()
            );

        for (int i = 0;
             i < places.size();
             i++) {

            result.get(i % days)
                    .add(
                            places.get(i)
                    );
        }

        return result;
    }

    private List<Place> applyWeatherRules(List<Place> list,
                                          WeatherType weather) {
        return list;
    }

    private List<Place> optimizeDayRoute(List<Place> dayPlaces) {
        return dayPlaces;
    }

    private String serialize(List<String> places) {
        try {
            return mapper.writeValueAsString(places);
        } catch (Exception e) {
            return "[]";
        }
    }

    // =====================================================
    // VERSION CREATE LOGIC
    // =====================================================
    private void createVersion(
            UUID tripId,
            int dayNumber,
            List<String> names,
            int confidence,
            String source
    ) {

        // 1️⃣ Find latest version number for this day
        ItineraryVersion latest =
                versionRepository
                        .findTopByTripIdAndDayNumberOrderByVersionNumberDesc(
                                tripId,
                                dayNumber
                        );

        int nextVersion =
                latest != null
                        ? latest.getVersionNumber() + 1
                        : 1;

        // 2️⃣ Create itinerary version
        ItineraryVersion version =
                ItineraryVersion.builder()
                        .tripId(tripId)
                        .dayNumber(dayNumber)
                        .versionNumber(nextVersion)
                        .placesJson(serialize(names))
                        .confidenceScore(confidence)
                        .source(source)
                        .createdAt(LocalDateTime.now())
                        .build();

        ItineraryVersion savedVersion =
                versionRepository.save(version);

        // 3️⃣ Create version-day entry (lock-aware)
        ItineraryVersionDay versionDay =
                new ItineraryVersionDay();

        versionDay.setItineraryVersionId(savedVersion.getId());
        versionDay.setDayNumber(dayNumber);
        versionDay.setPlacesJson(serialize(names));
        versionDay.setLocked(false);

        versionDayRepository.save(versionDay);
    }
}
