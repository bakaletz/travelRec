package com.travelRec.mapper;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.entity.UserPreferences;
import org.springframework.stereotype.Component;

@Component
public class PreferencesMapper {

    public PreferencesResponse toResponse(UserPreferences entity) {
        return PreferencesResponse.builder()
                .id(entity.getId())
                .cultureWeight(entity.getCultureWeight())
                .foodWeight(entity.getFoodWeight())
                .nightlifeWeight(entity.getNightlifeWeight())
                .natureWeight(entity.getNatureWeight())
                .safetyWeight(entity.getSafetyWeight())
                .budgetWeight(entity.getBudgetWeight())
                .beachWeight(entity.getBeachWeight())
                .architectureWeight(entity.getArchitectureWeight())
                .shoppingWeight(entity.getShoppingWeight())
                .preferredCityTypes(entity.getPreferredCityTypes())
                .preferredClimateTypes(entity.getPreferredClimateTypes())
                .build();
    }

    public void updateEntity(UserPreferences entity, PreferencesRequest request) {
        if (request.getCultureWeight() != null) entity.setCultureWeight(request.getCultureWeight());
        if (request.getFoodWeight() != null) entity.setFoodWeight(request.getFoodWeight());
        if (request.getNightlifeWeight() != null) entity.setNightlifeWeight(request.getNightlifeWeight());
        if (request.getNatureWeight() != null) entity.setNatureWeight(request.getNatureWeight());
        if (request.getSafetyWeight() != null) entity.setSafetyWeight(request.getSafetyWeight());
        if (request.getBudgetWeight() != null) entity.setBudgetWeight(request.getBudgetWeight());
        if (request.getBeachWeight() != null) entity.setBeachWeight(request.getBeachWeight());
        if (request.getArchitectureWeight() != null) entity.setArchitectureWeight(request.getArchitectureWeight());
        if (request.getShoppingWeight() != null) entity.setShoppingWeight(request.getShoppingWeight());
        if (request.getPreferredCityTypes() != null) entity.setPreferredCityTypes(request.getPreferredCityTypes());
        if (request.getPreferredClimateTypes() != null) entity.setPreferredClimateTypes(request.getPreferredClimateTypes());
    }
}