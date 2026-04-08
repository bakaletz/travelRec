package com.travelRec.dto.country;

import com.travelRec.entity.enums.Continent;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CountryResponse {

    private Long id;
    private String name;
    private String code;
    private Continent continent;
    private String language;
    private String currency;
    private String description;
    private String imageUrl;
    private Integer cityCount;
}
