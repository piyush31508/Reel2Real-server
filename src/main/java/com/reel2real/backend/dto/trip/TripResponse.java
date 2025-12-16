package com.reel2real.backend.dto.trip;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class TripResponse {

    private UUID id;
    private String destinationCity;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer totalDays;
    private String travelStyle;
}
