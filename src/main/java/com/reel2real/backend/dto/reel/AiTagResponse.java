package com.reel2real.backend.dto.reel;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AiTagResponse {
    private String placeName;
    private String city;
    private String country;
    private String category;
}

