package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reel {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;


    @Column(name = "reel_url", nullable = false)
    private String reelUrl;

    @Column(length = 50)
    private String platform; // instagram, youtube, fb, etc.

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "place_id")
    private Place place;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
