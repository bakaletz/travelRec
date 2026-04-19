package com.travelRec.dto.trip;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TripCityResponse {

    private Long id;
    private Long cityId;
    private String cityName;
    private String countryName;
    private String imageUrl;
    private Integer visitOrder;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private Double latitude;
    private Double longitude;
    private Double distanceFromPrevious;
    private Boolean suboptimalOrder;
}
