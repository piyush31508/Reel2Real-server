package com.reel2real.backend.dto.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TripCreateRequest {

    @NotBlank(message = "Destination city is required")
    private String destinationCity;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Positive(message = "Total days must be positive")
    private Integer totalDays;

    private String travelStyle; // relaxed / packed
}
