package com.reel2real.backend.dto.itinerary;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ItineraryResponse {

    private int dayNumber;
    private List<String> places;
}
