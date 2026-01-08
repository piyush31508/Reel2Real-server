package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_feedback")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryFeedback {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID itineraryVersionId;   // 🔥 NEW

    @Column(nullable = false)
    private Integer dayNumber;

    private UUID placeId; // nullable → whole day dislike

    @Column(nullable = false)
    private String feedbackType; // DISLIKE

    @Column(length = 500)
    private String reason;
}

