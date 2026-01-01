package com.reel2real.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reel2real.backend.entity.Place;
import com.reel2real.backend.integration.NominatimClient;
import com.reel2real.backend.integration.OpenRouterClient;
import com.reel2real.backend.repository.PlaceRepository;
import com.reel2real.backend.service.PlaceEnrichmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceEnrichmentServiceImpl implements PlaceEnrichmentService {

    private final PlaceRepository placeRepository;
    private final NominatimClient nominatimClient;
    private final OpenRouterClient openRouterClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    @Transactional
    @Override
    public void enrichPlaceAsync(UUID placeId) {

        Place place = placeRepository.findById(placeId).orElse(null);
        if (place == null) return;

        // 🔒 Skip already enriched places
        if ("ENRICHED".equals(place.getEnrichmentStatus())) {
            return;
        }

        try {
            log.info("Starting enrichment for place: {}", place.getName());

            // ======================
            // 1️⃣ GEO ENRICHMENT
            // ======================
            if (place.getLatitude() == null || place.getLongitude() == null) {
                double[] latLng = nominatimClient.getLatLng(
                        place.getName(),
                        place.getCity(),
                        place.getCountry()
                );

                if (latLng != null) {
                    place.setLatitude(latLng[0]);
                    place.setLongitude(latLng[1]);
                }
            }

            // ======================
            // 2️⃣ AI METADATA
            // ======================
            if (isMissingAIFields(place)) {

                String prompt = """
                You are a travel intelligence system.
                Enrich the following place with realistic metadata.
                Return ONLY valid JSON. No markdown.

                Place: %s
                City: %s
                Country: %s

                {
                  "activity": "",
                  "crowdLevel": "",
                  "safetyNote": "",
                  "season": "",
                  "budgetLevel": ""
                }
                """.formatted(
                        place.getName(),
                        place.getCity(),
                        place.getCountry()
                );

                String raw = openRouterClient.generate(prompt);
                String cleaned = sanitizeJson(raw);

                @SuppressWarnings("unchecked")
                Map<String, Object> ai =
                        objectMapper.readValue(cleaned, Map.class);

                place.setActivity((String) ai.get("activity"));
                place.setCrowdLevel((String) ai.get("crowdLevel"));
                place.setSafetyNote((String) ai.get("safetyNote"));
                place.setSeason((String) ai.get("season"));
                place.setBudgetLevel((String) ai.get("budgetLevel"));
            }

            place.setEnrichmentStatus("ENRICHED");
            place.setLastEnrichedAt(LocalDateTime.now());
            placeRepository.save(place);

            log.info("Enrichment completed for place: {}", place.getName());

        } catch (Exception e) {
            log.error("Enrichment failed for place {}", place.getName(), e);

            place.setEnrichmentStatus("FAILED");
            placeRepository.save(place);
        }
    }

    // ======================
    // HELPERS
    // ======================

    private boolean isMissingAIFields(Place place) {
        return place.getActivity() == null
                || place.getCrowdLevel() == null
                || place.getSafetyNote() == null
                || place.getSeason() == null
                || place.getBudgetLevel() == null;
    }

    private String sanitizeJson(String raw) {
        return raw
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }

    @Override
    public void enrichPlaceAsync(org.hibernate.validator.constraints.UUID placeId) {

    }
}
