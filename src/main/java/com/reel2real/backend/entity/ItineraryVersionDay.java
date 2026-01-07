package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "itinerary_version_days")
@Getter
@Setter
public class ItineraryVersionDay {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID itineraryVersionId;
    private int dayNumber;

    @Column(columnDefinition = "jsonb")
    private String placesJson;

    private boolean locked;
}
