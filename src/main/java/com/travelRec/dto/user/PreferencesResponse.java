package com.travelRec.dto.user;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import lombok.*;

import java.util.Set;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PreferencesResponse {

    private Long id;
    private Float cultureWeight;
    private Float foodWeight;
    private Float nightlifeWeight;
    private Float natureWeight;
    private Float safetyWeight;
    private Float budgetWeight;
    private Float beachWeight;
    private Float architectureWeight;
    private Float shoppingWeight;
    private Set<CityType> preferredCityTypes;
    private Set<ClimateType> preferredClimateTypes;
}
