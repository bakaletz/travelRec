package com.travelRec.dto.country;

import com.travelRec.entity.enums.Continent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CountryRequest {

    @NotBlank(message = "Country name is required")
    private String name;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 3, message = "Country code must be 2-3 characters")
    private String code;

    @NotNull(message = "Continent is required")
    private Continent continent;

    private String language;
    private String currency;
    private String description;
    private String imageUrl;
}
