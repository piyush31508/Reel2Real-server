package com.reel2real.backend.dto.trip;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class TripPlaceRequest {

    @NotNull(message = "Place id is required")
    private UUID placeId;

    // Optional priority (lower = higher priority)
    private Integer priority;
}
