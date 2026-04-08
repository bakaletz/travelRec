package com.travelRec.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DetailedRatingRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotNull(message = "City ID is required")
    private Long cityId;

    @NotNull @Min(1) @Max(5)
    private Integer overallScore;

    @Min(1) @Max(5)
    private Integer cultureRating;

    @Min(1) @Max(5)
    private Integer foodRating;

    @Min(1) @Max(5)
    private Integer nightlifeRating;

    @Min(1) @Max(5)
    private Integer natureRating;

    @Min(1) @Max(5)
    private Integer safetyRating;

    @Min(1) @Max(5)
    private Integer costRating;

    @Min(1) @Max(5)
    private Integer beachRating;

    @Min(1) @Max(5)
    private Integer architectureRating;

    @Min(1) @Max(5)
    private Integer shoppingRating;

    private String feedback;
}
