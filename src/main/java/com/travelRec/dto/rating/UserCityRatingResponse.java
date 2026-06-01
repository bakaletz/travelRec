package com.travelRec.dto.rating;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserCityRatingResponse {

    private int ratingCount;
    private Double overallScore;
    private Double cultureRating;
    private Double foodRating;
    private Double nightlifeRating;
    private Double natureRating;
    private Double safetyRating;
    private Double costRating;
    private Double beachRating;
    private Double architectureRating;
    private Double shoppingRating;
}