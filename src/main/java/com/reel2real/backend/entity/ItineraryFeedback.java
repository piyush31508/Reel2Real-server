package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_feedback")
@Data
@Getter
@Setter
@Builder
public class ItineraryFeedback {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID tripId;

    @Column(nullable = false)
    private Integer dayNumber;

    private UUID placeId; // nullable

    @Column(nullable = false)
    private String feedbackType; // LIKE / DISLIKE

    @Column(length = 500)
    private String reason;

}
