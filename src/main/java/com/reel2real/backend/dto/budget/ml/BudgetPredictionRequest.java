// BudgetPredictionRequest.java
package com.reel2real.backend.dto.budget.ml;

import lombok.Data;

@Data
public class BudgetPredictionRequest {
    private String city;
    private int duration;
    private String style;
    private String season;
}
