package com.reel2real.backend.dto.reel;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ReelResponse {

    private UUID id;
    private String reelUrl;
    private String platform;
    private UUID placeId;
    private String notes;
    private String placeName;
    private String country;
}
