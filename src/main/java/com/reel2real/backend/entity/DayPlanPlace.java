package com.reel2real.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "day_plan_places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayPlanPlace {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_plan_id", nullable = false)
    private DayPlan dayPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "time_slot", length = 50)
    private String timeSlot; // morning / afternoon / evening

    @Column(name = "visit_order")
    private Integer visitOrder;
}
