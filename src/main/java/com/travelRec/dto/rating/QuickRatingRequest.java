package com.travelRec.dto.rating;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class QuickRatingRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotNull(message = "City ID is required")
    private Long cityId;

    @NotNull(message = "Overall score is required")
    @Min(1) @Max(5)
    private Integer overallScore;

    private String feedback;
}
