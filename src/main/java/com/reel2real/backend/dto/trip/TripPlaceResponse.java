package com.reel2real.backend.dto.trip;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class TripPlaceResponse {

    private UUID id;        // trip_place id
    private UUID placeId;
    private String placeName;
    private String city;
    private Integer priority;
}
