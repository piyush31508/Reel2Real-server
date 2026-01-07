package com.reel2real.backend.itinerary;

import com.reel2real.backend.entity.Place;
import com.reel2real.backend.weather.WeatherType;

import java.util.List;

public class ConfidenceCalculator {

    public static int calculate(
            List<Place> places,
            WeatherType weather,
            String travelStyle,
            boolean budgetOk
    ) {
        int score = 0;

        // 1️⃣ Enrichment
        boolean allEnriched = places.stream()
                .allMatch(p -> "ENRICHED".equals(p.getEnrichmentStatus()));
        if (allEnriched) score += 25;

        // 2️⃣ Weather
        if (weather == WeatherType.CLEAR) score += 20;

        // 3️⃣ Distance (if geo exists)
        boolean geoReady = places.stream()
                .allMatch(p -> p.getLatitude() != null && p.getLongitude() != null);
        if (geoReady) score += 20;

        // 4️⃣ Travel style match
        boolean styleMatch = places.stream()
                .anyMatch(p ->
                        travelStyle != null &&
                                travelStyle.equalsIgnoreCase("relaxed") &&
                                "beach".equalsIgnoreCase(p.getCategory())
                );
        if (styleMatch) score += 20;

        // 5️⃣ Budget
        if (budgetOk) score += 15;

        return Math.min(score, 100);
    }

    public static String label(int score) {
        if (score >= 80) return "High confidence recommendation";
        if (score >= 60) return "Moderate confidence recommendation";
        return "Low confidence, may need adjustment";
    }
}
