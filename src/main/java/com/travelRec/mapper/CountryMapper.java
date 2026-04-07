package com.travelRec.mapper;

import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.Country;
import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

    public Country toEntity(CountryRequest request) {
        return Country.builder()
                .name(request.getName())
                .code(request.getCode())
                .continent(request.getContinent())
                .language(request.getLanguage())
                .currency(request.getCurrency())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
    }

    public CountryResponse toResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .continent(country.getContinent())
                .language(country.getLanguage())
                .currency(country.getCurrency())
                .description(country.getDescription())
                .imageUrl(country.getImageUrl())
                .cityCount(country.getCities() != null ? country.getCities().size() : 0)
                .build();
    }

    public void updateEntity(Country country, CountryRequest request) {
        country.setName(request.getName());
        country.setCode(request.getCode());
        country.setContinent(request.getContinent());
        country.setLanguage(request.getLanguage());
        country.setCurrency(request.getCurrency());
        country.setDescription(request.getDescription());
        country.setImageUrl(request.getImageUrl());
    }
}
