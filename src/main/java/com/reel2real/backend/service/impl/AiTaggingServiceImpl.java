package com.reel2real.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reel2real.backend.dto.reel.AiTagResponse;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.integration.OpenRouterClient;
import com.reel2real.backend.service.AiTaggingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiTaggingServiceImpl implements AiTaggingService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenRouterClient openRouterClient;

    private void applyBudgetDefaults(Place place) {

        if (place.getBudgetLevel() == null) return;

        switch (place.getBudgetLevel().toLowerCase()) {
            case "low" -> {
                place.setMinCost(200);
                place.setMaxCost(800);
            }
            case "medium" -> {
                place.setMinCost(800);
                place.setMaxCost(2500);
            }
            case "high" -> {
                place.setMinCost(2500);
                place.setMaxCost(8000);
            }
        }
    }

    @Override
    public AiTagResponse analyze(String caption, String hashtags) {

        String prompt = buildPrompt(caption, hashtags);
        String rawResponse = openRouterClient.generate(prompt);

        System.out.println("🔹 RAW LLM OUTPUT:\n" + rawResponse);

        AiTagResponse parsed = parseJson(rawResponse);
        normalize(parsed);

        System.out.println("🔹 PARSED OUTPUT:\n" + parsed);
        return parsed;
    }

    private String buildPrompt(String caption, String hashtags) {
        return """
You are a travel intelligence extractor.

Extract ONLY structured travel data.
Return STRICT JSON only.

Fields:
- placeName
- city
- country
- category
- activity
- season
- crowdLevel
- budgetLevel
- safetyNote

Caption: %s
Hashtags: %s

Output:
{
  "placeName": "",
  "city": "",
  "country": "",
  "category": "",
  "activity": "",
  "season": "",
  "crowdLevel": "",
  "budgetLevel": "",
  "safetyNote": ""
}
""".formatted(caption, hashtags);
    }

    private AiTagResponse parseJson(String raw) {
        try {
            String cleaned = raw
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            return mapper.readValue(cleaned, AiTagResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return AiTagResponse.builder().build();
        }
    }

    private void normalize(AiTagResponse r) {
        if (r == null) return;

        if (isBlank(r.getCity()) && !isBlank(r.getPlaceName())) {
            r.setCity(r.getPlaceName());
            r.setPlaceName(null);
        }

        if (isBlank(r.getPlaceName())) r.setPlaceName(null);
        if (isBlank(r.getCity())) r.setCity(null);
        if (isBlank(r.getCountry())) r.setCountry(null);
        if (isBlank(r.getCategory())) r.setCategory(null);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
