package com.reel2real.backend.dto.feedback;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FeedbackRequest {

    @NotNull
    private UUID tripId;

    @NotNull
    private Integer dayNumber;

    private UUID placeId; // optional

    @NotNull
    private String feedbackType; // LIKE / DISLIKE

    private String reason;
}
