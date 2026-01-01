package com.reel2real.backend.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class WeatherClient {

    @Value("${weather.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns HIGH-LEVEL weather:
     * Rain | Clear | Clouds | Snow | Extreme
     */
    @SuppressWarnings("unchecked")
    public String getWeather(String city) {

        String url =
                "https://api.openweathermap.org/data/2.5/forecast?q="
                        + city
                        + "&appid="
                        + apiKey
                        + "&units=metric";

        ResponseEntity<Map> response =
                restTemplate.getForEntity(url, Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null) return "Clear";

        List<Map<String, Object>> list =
                (List<Map<String, Object>>) body.get("list");

        if (list == null || list.isEmpty()) return "Clear";

        Map<String, Object> first = list.get(0);
        List<Map<String, Object>> weather =
                (List<Map<String, Object>>) first.get("weather");

        if (weather == null || weather.isEmpty()) return "Clear";

        return weather.get(0).get("main").toString();
    }
}
