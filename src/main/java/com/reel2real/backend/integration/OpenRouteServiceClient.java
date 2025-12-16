package com.reel2real.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenRouteServiceClient {

    @Value("${ors.api.key}")
    private String apiKey;

    @Value("${ors.api.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * @param coordinates List of [longitude, latitude]
     * @return distance matrix in kilometers
     */
    public double[][] getDistanceMatrix(List<double[]> coordinates) {

        String url = baseUrl + "/v2/matrix/driving-car";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("locations", coordinates);
        requestBody.put("metrics", List.of("distance"));
        requestBody.put("units", "km");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", apiKey);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response =
                restTemplate.postForEntity(url, request, Map.class);

        return extractDistanceMatrix(response.getBody());
    }

    @SuppressWarnings("unchecked")
    private double[][] extractDistanceMatrix(Map<String, Object> response) {

        List<List<Double>> distances =
                (List<List<Double>>) response.get("distances");

        int size = distances.size();
        double[][] matrix = new double[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = distances.get(i).get(j);
            }
        }
        return matrix;
    }
}
