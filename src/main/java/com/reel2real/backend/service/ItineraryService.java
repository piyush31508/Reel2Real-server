package com.reel2real.backend.service;

import com.reel2real.backend.dto.itinerary.ItineraryResponse;

import java.util.List;
import java.util.UUID;

public interface ItineraryService {

    List<ItineraryResponse> generateItinerary(UUID tripId);
}
