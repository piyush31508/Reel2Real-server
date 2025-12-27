package com.reel2real.backend.dto.reel;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTagResponse {

    private String placeName;
    private String city;
    private String country;
    private String category;

    private String activity;        // beach walk, cafe hopping
    private String season;          // summer / monsoon / winter
    private String crowdLevel;      // low / medium / high
    private String budgetLevel;     // low / medium / high
    private String safetyNote;      // night unsafe, crowded area
}
