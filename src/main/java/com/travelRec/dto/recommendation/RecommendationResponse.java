package com.travelRec.dto.recommendation;

import com.travelRec.dto.city.CityResponse;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RecommendationResponse {

    private CityResponse city;
    private Double similarityScore;
    private String reason;
}
