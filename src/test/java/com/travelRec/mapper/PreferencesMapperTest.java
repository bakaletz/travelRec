package com.travelRec.mapper;

import com.travelRec.dto.user.PreferencesRequest;
import com.travelRec.dto.user.PreferencesResponse;
import com.travelRec.entity.UserPreferences;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PreferencesMapperTest {

    private PreferencesMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PreferencesMapper();
    }

    private UserPreferences buildPreferences() {
        return UserPreferences.builder()
                .id(1L)
                .cultureWeight(0.9f)
                .foodWeight(0.8f)
                .nightlifeWeight(0.3f)
                .natureWeight(0.7f)
                .safetyWeight(0.2f)
                .budgetWeight(0.6f)
                .beachWeight(0.4f)
                .architectureWeight(0.5f)
                .shoppingWeight(0.1f)
                .preferredCityType(CityType.LARGE_CITY)
                .preferredClimate(ClimateType.MEDITERRANEAN)
                .build();
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all weights")
        void shouldMapAllWeights() {
            UserPreferences prefs = buildPreferences();
            PreferencesResponse response = mapper.toResponse(prefs);

            assertEquals(1L, response.getId());
            assertEquals(0.9f, response.getCultureWeight());
            assertEquals(0.8f, response.getFoodWeight());
            assertEquals(0.3f, response.getNightlifeWeight());
            assertEquals(0.7f, response.getNatureWeight());
            assertEquals(0.2f, response.getSafetyWeight());
            assertEquals(0.6f, response.getBudgetWeight());
            assertEquals(0.4f, response.getBeachWeight());
            assertEquals(0.5f, response.getArchitectureWeight());
            assertEquals(0.1f, response.getShoppingWeight());
        }

        @Test
        @DisplayName("should map preferred filters")
        void shouldMapPreferredFilters() {
            UserPreferences prefs = buildPreferences();
            PreferencesResponse response = mapper.toResponse(prefs);

            assertEquals(CityType.LARGE_CITY, response.getPreferredCityType());
            assertEquals(ClimateType.MEDITERRANEAN, response.getPreferredClimate());
        }

        @Test
        @DisplayName("should handle null filters")
        void shouldHandleNullFilters() {
            UserPreferences prefs = UserPreferences.builder()
                    .id(1L)
                    .build();

            PreferencesResponse response = mapper.toResponse(prefs);

            assertNull(response.getPreferredCityType());
            assertNull(response.getPreferredClimate());
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        @DisplayName("should update only provided fields")
        void shouldUpdateOnlyProvided() {
            UserPreferences prefs = buildPreferences();
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.1f)
                    .foodWeight(0.2f)
                    .build();

            mapper.updateEntity(prefs, request);

            assertEquals(0.1f, prefs.getCultureWeight());
            assertEquals(0.2f, prefs.getFoodWeight());
            assertEquals(0.3f, prefs.getNightlifeWeight());
            assertEquals(0.7f, prefs.getNatureWeight());
            assertEquals(0.2f, prefs.getSafetyWeight());
            assertEquals(0.6f, prefs.getBudgetWeight());
            assertEquals(0.4f, prefs.getBeachWeight());
            assertEquals(0.5f, prefs.getArchitectureWeight());
            assertEquals(0.1f, prefs.getShoppingWeight());
        }

        @Test
        @DisplayName("should not overwrite with null values")
        void shouldNotOverwriteWithNull() {
            UserPreferences prefs = buildPreferences();
            PreferencesRequest request = PreferencesRequest.builder().build();

            mapper.updateEntity(prefs, request);

            assertEquals(0.9f, prefs.getCultureWeight());
            assertEquals(0.8f, prefs.getFoodWeight());
            assertEquals(0.3f, prefs.getNightlifeWeight());
            assertEquals(CityType.LARGE_CITY, prefs.getPreferredCityType());
            assertEquals(ClimateType.MEDITERRANEAN, prefs.getPreferredClimate());
        }

        @Test
        @DisplayName("should update all fields when all provided")
        void shouldUpdateAllFields() {
            UserPreferences prefs = buildPreferences();
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.1f)
                    .foodWeight(0.1f)
                    .nightlifeWeight(0.1f)
                    .natureWeight(0.1f)
                    .safetyWeight(0.1f)
                    .budgetWeight(0.1f)
                    .beachWeight(0.1f)
                    .architectureWeight(0.1f)
                    .shoppingWeight(0.1f)
                    .preferredCityType(CityType.RESORT)
                    .preferredClimate(ClimateType.TROPICAL)
                    .build();

            mapper.updateEntity(prefs, request);

            assertEquals(0.1f, prefs.getCultureWeight());
            assertEquals(0.1f, prefs.getFoodWeight());
            assertEquals(0.1f, prefs.getNightlifeWeight());
            assertEquals(0.1f, prefs.getNatureWeight());
            assertEquals(0.1f, prefs.getSafetyWeight());
            assertEquals(0.1f, prefs.getBudgetWeight());
            assertEquals(0.1f, prefs.getBeachWeight());
            assertEquals(0.1f, prefs.getArchitectureWeight());
            assertEquals(0.1f, prefs.getShoppingWeight());
            assertEquals(CityType.RESORT, prefs.getPreferredCityType());
            assertEquals(ClimateType.TROPICAL, prefs.getPreferredClimate());
        }

        @Test
        @DisplayName("should preserve id after update")
        void shouldPreserveId() {
            UserPreferences prefs = buildPreferences();
            PreferencesRequest request = PreferencesRequest.builder()
                    .cultureWeight(0.1f)
                    .build();

            mapper.updateEntity(prefs, request);

            assertEquals(1L, prefs.getId());
        }
    }
}
