package com.travelRec.mapper;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.entity.UserPreferences;
import org.springframework.stereotype.Component;

@Component
public class PreferencesMapper {

    public PreferencesResponse toResponse(UserPreferences prefs) {
        return PreferencesResponse.builder()
                .id(prefs.getId())
                .cultureWeight(prefs.getCultureWeight())
                .foodWeight(prefs.getFoodWeight())
                .nightlifeWeight(prefs.getNightlifeWeight())
                .natureWeight(prefs.getNatureWeight())
                .safetyWeight(prefs.getSafetyWeight())
                .budgetWeight(prefs.getBudgetWeight())
                .beachWeight(prefs.getBeachWeight())
                .architectureWeight(prefs.getArchitectureWeight())
                .shoppingWeight(prefs.getShoppingWeight())
                .preferredCityType(prefs.getPreferredCityType())
                .preferredClimate(prefs.getPreferredClimate())
                .build();
    }

    public void updateEntity(UserPreferences prefs, PreferencesRequest request) {
        if (request.getCultureWeight() != null) prefs.setCultureWeight(request.getCultureWeight());
        if (request.getFoodWeight() != null) prefs.setFoodWeight(request.getFoodWeight());
        if (request.getNightlifeWeight() != null) prefs.setNightlifeWeight(request.getNightlifeWeight());
        if (request.getNatureWeight() != null) prefs.setNatureWeight(request.getNatureWeight());
        if (request.getSafetyWeight() != null) prefs.setSafetyWeight(request.getSafetyWeight());
        if (request.getBudgetWeight() != null) prefs.setBudgetWeight(request.getBudgetWeight());
        if (request.getBeachWeight() != null) prefs.setBeachWeight(request.getBeachWeight());
        if (request.getArchitectureWeight() != null) prefs.setArchitectureWeight(request.getArchitectureWeight());
        if (request.getShoppingWeight() != null) prefs.setShoppingWeight(request.getShoppingWeight());
        if (request.getPreferredCityType() != null) prefs.setPreferredCityType(request.getPreferredCityType());
        if (request.getPreferredClimate() != null) prefs.setPreferredClimate(request.getPreferredClimate());
    }
}
