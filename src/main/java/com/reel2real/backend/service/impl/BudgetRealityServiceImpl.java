package com.reel2real.backend.service.impl;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.service.BudgetRealityService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BudgetRealityServiceImpl implements BudgetRealityService {

    @Override
    public BudgetRealityResponse calculate(List<Place> places) {

        if (places == null || places.isEmpty()) {
            return null;
        }

        int min = 0;
        int max = 0;
        int reelExpectation = 0;

        for (Place place : places) {

            String category =
                    place.getCategory() != null
                            ? place.getCategory().toLowerCase()
                            : "";

            switch (category) {
                case "beach" -> {
                    min += 200;
                    max += 600;
                    reelExpectation += 150;
                }
                case "market" -> {
                    min += 300;
                    max += 800;
                    reelExpectation += 250;
                }
                case "monument" -> {
                    min += 100;
                    max += 300;
                    reelExpectation += 100;
                }
                case "food" -> {
                    min += 400;
                    max += 1200;
                    reelExpectation += 300;
                }
                case "nightlife" -> {
                    min += 800;
                    max += 2500;
                    reelExpectation += 500;
                }
                default -> {
                    min += 200;
                    max += 600;
                    reelExpectation += 150;
                }
            }
        }

        int realisticAvg = (min + max) / 2;
        int diffPercent =
                reelExpectation == 0
                        ? 0
                        : ((realisticAvg - reelExpectation) * 100) / reelExpectation;

        String level =
                diffPercent < 30 ? "OK"
                        : diffPercent < 80 ? "WARNING"
                        : "SHOCKING";

        String message =
                "This trip costs ~" + diffPercent +
                        "% more than what reels usually suggest";

        return BudgetRealityResponse.builder()
                .estimatedMinCost(min)
                .estimatedMaxCost(max)
                .reelExpectation(reelExpectation)
                .realisticAvgCost(realisticAvg)
                .differencePercent(diffPercent)
                .realityCheck(level)
                .message(message)
                .build();
    }
}
