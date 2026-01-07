package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_feedback")
@Data
public class ItineraryFeedback {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID tripId;
    private int dayNumber;

    private UUID placeId; // nullable

    private String feedbackType; // LIKE / DISLIKE
    private String reason;

    private LocalDateTime createdAt = LocalDateTime.now();
}
