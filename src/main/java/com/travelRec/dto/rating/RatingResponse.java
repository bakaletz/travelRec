package com.travelRec.dto.rating;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RatingResponse {

    private Long id;
    private Long tripId;
    private Long cityId;
    private String cityName;
    private Integer overallScore;
    private Integer cultureRating;
    private Integer foodRating;
    private Integer nightlifeRating;
    private Integer natureRating;
    private Integer safetyRating;
    private Integer costRating;
    private Integer beachRating;
    private Integer architectureRating;
    private Integer shoppingRating;
    private String feedback;
    private boolean detailed;
    private LocalDateTime createdAt;
}
