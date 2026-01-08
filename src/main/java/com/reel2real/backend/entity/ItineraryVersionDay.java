package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
        name = "itinerary_version_days",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"itinerary_version_id", "day_number"})
        }
)
@Getter
@Setter
public class ItineraryVersionDay {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "itinerary_version_id", nullable = false)
    private UUID itineraryVersionId;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "places", nullable = false)
    private String placesJson;

    @Column(nullable = false)
    private boolean locked = false;
}

