package com.travelRec.dto.user;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PreferencesRequest {

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float cultureWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float foodWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float nightlifeWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float natureWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float safetyWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float budgetWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float beachWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float architectureWeight;

    @DecimalMin("0.0") @DecimalMax("1.0")
    private Float shoppingWeight;

    private CityType preferredCityType;
    private ClimateType preferredClimate;
}
