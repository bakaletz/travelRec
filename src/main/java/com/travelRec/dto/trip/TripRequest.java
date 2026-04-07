package com.travelRec.dto.trip;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TripRequest {

    @NotBlank(message = "Trip name is required")
    private String name;

    private LocalDate startDate;
    private LocalDate endDate;
}
