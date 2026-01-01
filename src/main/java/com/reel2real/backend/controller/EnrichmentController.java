package com.reel2real.backend.controller;

import com.reel2real.backend.service.PlaceEnrichmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrichment")
public class EnrichmentController {

    private final PlaceEnrichmentService enrichmentService;

    @PostMapping("/place/{id}")
    public ResponseEntity<String> enrich(@PathVariable UUID id) {
        enrichmentService.enrichPlaceAsync(id);
        return ResponseEntity.ok("Enrichment started");
    }
}
