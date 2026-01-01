package com.reel2real.backend.service;

import java.util.UUID;

public interface PlaceEnrichmentService {
    void enrichPlaceAsync(UUID placeId);
}

