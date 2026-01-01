package com.reel2real.backend.service;

import org.hibernate.validator.constraints.UUID;

public interface PlaceEnrichmentService {
    void enrichPlaceAsync(UUID placeId);
}

