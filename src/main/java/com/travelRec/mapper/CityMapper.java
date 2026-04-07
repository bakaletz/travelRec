package com.travelRec.mapper;

import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Country;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {

    public City toEntity(CityRequest request, Country country) {
        return City.builder()
                .country(country)
                .name(request.getName())
                .region(request.getRegion())
                .cityType(request.getCityType())
                .population(request.getPopulation())
                .climateType(request.getClimateType())
                .avgTempSummer(request.getAvgTempSummer())
                .avgTempWinter(request.getAvgTempWinter())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .baseCostLevel(request.getBaseCostLevel())
                .baseSafetyScore(request.getBaseSafetyScore())
                .baseCultureScore(request.getBaseCultureScore())
                .baseFoodScore(request.getBaseFoodScore())
                .baseNightlifeScore(request.getBaseNightlifeScore())
                .baseNatureScore(request.getBaseNatureScore())
                .baseBeachScore(request.getBaseBeachScore())
                .baseArchitectureScore(request.getBaseArchitectureScore())
                .baseShoppingScore(request.getBaseShoppingScore())
                .publicTransportScore(request.getPublicTransportScore())
                .walkabilityScore(request.getWalkabilityScore())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();
    }

    public CityResponse toResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .name(city.getName())
                .region(city.getRegion())
                .cityType(city.getCityType())
                .population(city.getPopulation())
                .climateType(city.getClimateType())
                .avgTempSummer(city.getAvgTempSummer())
                .avgTempWinter(city.getAvgTempWinter())
                .latitude(city.getLatitude())
                .longitude(city.getLongitude())
                .costLevel(city.getCostLevel())
                .safetyScore(city.getSafetyScore())
                .cultureScore(city.getCultureScore())
                .foodScore(city.getFoodScore())
                .nightlifeScore(city.getNightlifeScore())
                .natureScore(city.getNatureScore())
                .beachScore(city.getBeachScore())
                .architectureScore(city.getArchitectureScore())
                .shoppingScore(city.getShoppingScore())
                .publicTransportScore(city.getPublicTransportScore())
                .walkabilityScore(city.getWalkabilityScore())
                .popularity(city.getPopularity())
                .ratingCount(city.getRatingCount())
                .description(city.getDescription())
                .imageUrl(city.getImageUrl())
                .countryId(city.getCountry().getId())
                .countryName(city.getCountry().getName())
                .countryCode(city.getCountry().getCode())
                .build();
    }

    public void updateEntity(City city, CityRequest request) {
        city.setName(request.getName());
        city.setRegion(request.getRegion());
        city.setCityType(request.getCityType());
        city.setPopulation(request.getPopulation());
        city.setClimateType(request.getClimateType());
        city.setAvgTempSummer(request.getAvgTempSummer());
        city.setAvgTempWinter(request.getAvgTempWinter());
        city.setLatitude(request.getLatitude());
        city.setLongitude(request.getLongitude());
        city.setBaseCostLevel(request.getBaseCostLevel());
        city.setBaseSafetyScore(request.getBaseSafetyScore());
        city.setBaseCultureScore(request.getBaseCultureScore());
        city.setBaseFoodScore(request.getBaseFoodScore());
        city.setBaseNightlifeScore(request.getBaseNightlifeScore());
        city.setBaseNatureScore(request.getBaseNatureScore());
        city.setBaseBeachScore(request.getBaseBeachScore());
        city.setBaseArchitectureScore(request.getBaseArchitectureScore());
        city.setBaseShoppingScore(request.getBaseShoppingScore());
        city.setPublicTransportScore(request.getPublicTransportScore());
        city.setWalkabilityScore(request.getWalkabilityScore());
        city.setDescription(request.getDescription());
        city.setImageUrl(request.getImageUrl());
    }
}