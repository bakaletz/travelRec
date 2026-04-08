package com.travelRec.dto.trip;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AddCityToTripRequest {

    @NotNull(message = "City ID is required")
    private Long cityId;

    @NotNull(message = "Visit order is required")
    private Integer visitOrder;

    private LocalDate arrivalDate;
    private LocalDate departureDate;
}
