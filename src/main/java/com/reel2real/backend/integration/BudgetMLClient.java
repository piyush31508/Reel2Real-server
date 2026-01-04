package com.reel2real.backend.integration;

import com.reel2real.backend.dto.budget.ml.BudgetPredictionRequest;
import com.reel2real.backend.dto.budget.ml.BudgetPredictionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class BudgetMLClient {

    @Value("${budget.ml.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public BudgetPredictionResponse predict(BudgetPredictionRequest request) {

        return restTemplate.postForObject(
                baseUrl + "/predict-budget",
                request,
                BudgetPredictionResponse.class
        );
    }
}
