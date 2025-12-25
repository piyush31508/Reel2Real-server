package com.reel2real.backend.dto.reel;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReelCreateRequest {

    @NotBlank(message = "Reel URL is required")
    private String reelUrl;

    private String platform; // instagram / youtube
    private String notes;

    private String caption;  // 👉 used for auto-tag
    private String hashtags; // 👉 used for auto-tag

    // optional
    private String placeName;
    private String city;
    private String country;
}
