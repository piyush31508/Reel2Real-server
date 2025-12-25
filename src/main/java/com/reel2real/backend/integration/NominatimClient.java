package com.reel2real.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Component
public class NominatimClient {

    @Value("${nominatim.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public double[] getLatLng(String place, String city) {

        // Try: "Place, City"
        double[] result = call(place + ", " + city);
        if (result != null) return result;

        // Fallback: "Place"
        return call(place);
    }

    @SuppressWarnings("unchecked")
    private double[] call(String query) {

        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/search")
                .queryParam("q", query)
                .queryParam("format", "json")
                .queryParam("limit", 1)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Reel2Real/1.0");
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<List> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        List.class
                );

        if (response.getBody() == null || response.getBody().isEmpty()) {
            return null;
        }

        Map<String, Object> loc =
                (Map<String, Object>) response.getBody().get(0);

        return new double[]{
                Double.parseDouble(loc.get("lat").toString()),
                Double.parseDouble(loc.get("lon").toString())
        };
    }
}
