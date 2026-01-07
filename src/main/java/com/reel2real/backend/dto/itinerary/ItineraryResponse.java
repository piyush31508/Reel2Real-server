package com.reel2real.backend.dto.itinerary;

import com.reel2real.backend.dto.budget.BudgetRealityResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItineraryResponse {

    private int dayNumber;
    private List<String> places;

    // 🔥 Explainability
    private String reason;

    private Integer confidenceScore;
    private String confidenceLabel;

    // 🔥 Budget Reality Meter (ONLY on Day 1)
    private BudgetRealityResponse budgetReality;
}
