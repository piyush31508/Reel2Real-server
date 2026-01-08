package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(length = 2000)
    private String placesJson;   // serialized list

    private Integer confidenceScore;

    @Column(nullable = false)
    private String source;       // ORIGINAL / REGENERATED

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime createdAt;
}
