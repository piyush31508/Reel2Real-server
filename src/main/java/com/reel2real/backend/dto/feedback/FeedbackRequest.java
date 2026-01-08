package com.reel2real.backend.dto.feedback;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class FeedbackRequest {

    @NotNull
    private UUID tripId;

    @NotNull
    private Integer dayNumber;

    private UUID placeId;

    private String feedbackType = "DISLIKE";

    private String reason;
}
