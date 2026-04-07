package com.travelRec.dto.trip;

import com.travelRec.entity.enums.TripStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class TripResponse {

    private Long id;
    private String name;
    private TripStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private List<TripCityResponse> cities;
}
