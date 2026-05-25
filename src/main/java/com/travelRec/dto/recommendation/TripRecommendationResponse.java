package com.travelRec.dto.recommendation;

import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.enums.CityType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TripRecommendationResponse {
    private List<CityResponse> cities;
    private Double tripScore;
    private Double relevanceScore;
    private Double coherenceScore;
    private Integer suggestedDurationDays;
    private Double totalDistanceKm;
    private CityType dominantCityType;
    private String reason;
}