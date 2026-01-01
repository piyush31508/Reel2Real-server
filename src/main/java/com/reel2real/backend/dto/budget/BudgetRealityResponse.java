package com.reel2real.backend.dto.budget;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BudgetRealityResponse {

    private int estimatedMinCost;
    private int estimatedMaxCost;

    // 🔥 NEW
    private int reelExpectation;
    private int realisticAvgCost;
    private int differencePercent;

    private String realityCheck; // OK | WARNING | SHOCKING
    private String message;
}
