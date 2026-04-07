package com.travelRec.dto.city;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CityResponse {

    private Long id;
    private String name;
    private String region;
    private CityType cityType;
    private Integer population;
    private ClimateType climateType;
    private Float avgTempSummer;
    private Float avgTempWinter;
    private Double latitude;
    private Double longitude;
    private Float costLevel;
    private Float safetyScore;
    private Float cultureScore;
    private Float foodScore;
    private Float nightlifeScore;
    private Float natureScore;
    private Float beachScore;
    private Float architectureScore;
    private Float shoppingScore;
    private Float publicTransportScore;
    private Float walkabilityScore;
    private Float popularity;
    private Integer ratingCount;
    private String description;
    private String imageUrl;
    private Long countryId;
    private String countryName;
    private String countryCode;
}