package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.dto.budget.ml.BudgetPredictionRequest;
import com.reel2real.backend.dto.budget.ml.BudgetPredictionResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.entity.Trip;
import com.reel2real.backend.integration.BudgetMLClient;
import com.reel2real.backend.service.BudgetRealityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BudgetRealityServiceImpl implements BudgetRealityService {

    private final BudgetMLClient budgetMLClient;

    @Override
    public BudgetRealityResponse calculate(List<Place> places, Trip trip) {

        if (places == null || places.isEmpty()) {
            return null;
        }

        // ==============================
        // 1️⃣ BUILD ML REQUEST
        // ==============================
        BudgetPredictionRequest request = new BudgetPredictionRequest();
        request.setCity(trip.getDestinationCity());
        request.setDuration(trip.getTotalDays());
        request.setStyle(
                trip.getTravelStyle() != null
                        ? trip.getTravelStyle()
                        : "budget"
        );
        request.setSeason("peak"); // later auto-detect

        BudgetPredictionResponse mlResponse;

        try {
            mlResponse = budgetMLClient.predict(request);
        } catch (Exception e) {
            log.error("ML Budget API failed, falling back", e);
            return fallbackBudget(places);
        }

        double mlEstimatedCost = mlResponse.getEstimatedCost();

        // ==============================
        // 2️⃣ REEL EXPECTATION (INTENTIONALLY LOW)
        // ==============================
        int reelExpectation = calculateReelExpectation(places);

        int diffPercent =
                reelExpectation == 0
                        ? 0
                        : (int) (((mlEstimatedCost - reelExpectation) * 100) / reelExpectation);

        // ==============================
        // 3️⃣ REALITY CLASSIFICATION
        // ==============================
        String level =
                diffPercent < 30 ? "OK"
                        : diffPercent < 80 ? "WARNING"
                        : "SHOCKING";

        String message =
                "Reels hide real travel costs. ML-based estimate applied.";

        // ==============================
        // 4️⃣ FINAL RESPONSE
        // ==============================
        return BudgetRealityResponse.builder()
                .estimatedMinCost((int) (mlEstimatedCost * 0.85))
                .estimatedMaxCost((int) (mlEstimatedCost * 1.15))
                .reelExpectation(reelExpectation)
                .realisticAvgCost((int) mlEstimatedCost)
                .differencePercent(diffPercent)
                .realityCheck(level)
                .message(message)
                .build();
    }

    // ==============================
    // HELPERS
    // ==============================

    /**
     * What reels usually suggest (optimistic & misleading)
     */
    private int calculateReelExpectation(List<Place> places) {
        return places.size() * 300;
    }

    /**
     * Fallback logic if ML API is down
     */
    private BudgetRealityResponse fallbackBudget(List<Place> places) {

        int min = places.size() * 800;
        int max = places.size() * 2000;
        int avg = (min + max) / 2;

        return BudgetRealityResponse.builder()
                .estimatedMinCost(min)
                .estimatedMaxCost(max)
                .reelExpectation(places.size() * 300)
                .realisticAvgCost(avg)
                .differencePercent(100)
                .realityCheck("WARNING")
                .message("Fallback estimation used due to ML unavailability")
                .build();
    }
}
