package com.travelRec.dto.city;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CityRequest {

    @NotNull(message = "Country ID is required")
    private Long countryId;

    @NotBlank(message = "City name is required")
    private String name;

    private String region;

    @NotNull(message = "City type is required")
    private CityType cityType;

    private Integer population;

    @NotNull(message = "Climate type is required")
    private ClimateType climateType;

    private Float avgTempSummer;
    private Float avgTempWinter;

    @NotNull(message = "Latitude is required")
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double longitude;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseCostLevel;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseSafetyScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseCultureScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseFoodScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseNightlifeScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseNatureScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseBeachScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseArchitectureScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float baseShoppingScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float publicTransportScore;

    @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
    private Float walkabilityScore;

    private String description;
    private String imageUrl;
}