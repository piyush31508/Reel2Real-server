package com.reel2real.backend.controller;

import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reel2real.backend.dto.itinerary.ItineraryResponse;
import com.reel2real.backend.entity.ItineraryVersion;
import com.reel2real.backend.repository.ItineraryVersionRepository;
import com.reel2real.backend.repository.TripRepository;
import com.reel2real.backend.service.ItineraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/itinerary")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final ItineraryVersionRepository versionRepository;
    private final TripRepository tripRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    // =====================================================
    // PHASE 2 – Generate (SAME ENDPOINT)
    // =====================================================
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<ItineraryResponse>> generateItinerary(
            @PathVariable UUID tripId
    ) {
        List<ItineraryResponse> result =
                itineraryService.generateItinerary(tripId);

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // PHASE 3 – Regenerate (SAME ENDPOINT)
    // =====================================================
    @PostMapping("/trip/{tripId}/regenerate")
    public ResponseEntity<List<ItineraryResponse>> regenerateItinerary(
            @PathVariable UUID tripId
    ) {
        List<ItineraryResponse> result =
                itineraryService.regenerateItinerary(tripId);

        return ResponseEntity.ok(result);
    }

    // =====================================================
    // 🔥 PHASE 3 – NEW: Lock Day Endpoint
    // (ONLY ADDITION – prev code safe)
    // =====================================================
    @PostMapping("/trip/{tripId}/lock-day/{dayNumber}")
    public ResponseEntity<Void> lockDay(
            @PathVariable UUID tripId,
            @PathVariable int dayNumber
    ) {

        if (dayNumber <= 0) {
            throw new IllegalArgumentException("Day number must be ≥ 1");
        }

        itineraryService.lockDay(tripId, dayNumber);

        return ResponseEntity.ok().build();
    }

    //Get All version

    @GetMapping("/versions/{tripId}")
    public ResponseEntity<List<ItineraryVersion>> versions(
            @PathVariable UUID tripId
    ) {
        return ResponseEntity.ok(
                versionRepository.findByTripIdOrderByCreatedAtDesc(tripId)
        );
    }

    // COMPARE 2 VERSIONS
    // -------------------------------
    @GetMapping("/compare")
    public ResponseEntity<Map<String,Object>> compare(
            @RequestParam UUID v1,
            @RequestParam UUID v2
    ) throws Exception {

        ItineraryVersion a =
                versionRepository.findById(v1)
                        .orElseThrow();

        ItineraryVersion b =
                versionRepository.findById(v2)
                        .orElseThrow();

        ObjectMapper m = new ObjectMapper();

        List<String> p1 =
                List.of(
                        m.readValue(a.getPlacesJson(), String[].class)
                );

        List<String> p2 =
                List.of(
                        m.readValue(b.getPlacesJson(), String[].class)
                );

        Map<String,Object> res = new HashMap<>();
        res.put("version1", p1);
        res.put("version2", p2);

        return ResponseEntity.ok(res);
    }

    // REVERT TO VERSION
    // -------------------------------
    @PostMapping("/revert/{versionId}")
    public ResponseEntity<Void> revert(
            @PathVariable UUID versionId
    ) {

        ItineraryVersion v =
                versionRepository.findById(versionId)
                        .orElseThrow();

        // simple audit purpose
        tripRepository.findById(v.getTripId())
                .orElseThrow();

        // NEXT phase me actual revert logic
        return ResponseEntity.ok().build();
    }

    // HELPER – SAVE VERSION LOGIC
    // ==================================================

    private void saveVersionBatch(
            UUID tripId,
            List<ItineraryResponse> list,
            String source
    ) {

        for (ItineraryResponse r : list) {

            int nextVersion =
                    resolveNextVersion(tripId, r.getDayNumber());

            ItineraryVersion version =
                    ItineraryVersion.builder()
                            .tripId(tripId)
                            .dayNumber(r.getDayNumber())
                            .versionNumber(nextVersion)
                            .placesJson(
                                    serialize(r.getPlaces())
                            )
                            .confidenceScore(r.getConfidenceScore())
                            .source(source)
                            .createdAt(LocalDateTime.now())
                            .build();

            versionRepository.save(version);
        }
    }

    private int resolveNextVersion(
            UUID tripId,
            Integer dayNumber
    ) {

        ItineraryVersion top =
                versionRepository
                        .findTopByTripIdAndDayNumberOrderByVersionNumberDesc(
                                tripId, dayNumber
                        );

        return top != null
                ? top.getVersionNumber() + 1
                : 1;
    }

    private String serialize(List<String> places){
        try{
            return mapper.writeValueAsString(places);
        }catch(Exception e){
            return "[]";
        }
    }
}
