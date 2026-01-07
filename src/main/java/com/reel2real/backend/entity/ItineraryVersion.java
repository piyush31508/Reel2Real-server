package com.reel2real.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_versions")
@Getter
@Setter
public class ItineraryVersion {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID tripId;
    private int versionNumber;
    private String reason;

    private LocalDateTime createdAt;
}
