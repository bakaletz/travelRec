package com.travelRec.mapper;

import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CityMapperTest {

    private CityMapper mapper;
    private Country country;

    @BeforeEach
    void setUp() {
        mapper = new CityMapper();
        country = Country.builder()
                .id(1L)
                .name("Ukraine")
                .code("UA")
                .continent(Continent.EUROPE)
                .build();
    }

    private CityRequest buildRequest() {
        return CityRequest.builder()
                .countryId(1L)
                .name("Kyiv")
                .region("Central Ukraine")
                .cityType(CityType.MEGAPOLIS)
                .population(2950000)
                .climateType(ClimateType.CONTINENTAL)
                .avgTempSummer(25.0f)
                .avgTempWinter(-3.0f)
                .latitude(50.4501)
                .longitude(30.5234)
                .baseCostLevel(0.35f)
                .baseSafetyScore(0.6f)
                .baseCultureScore(0.85f)
                .baseFoodScore(0.75f)
                .baseNightlifeScore(0.8f)
                .baseNatureScore(0.5f)
                .baseBeachScore(0.1f)
                .baseArchitectureScore(0.8f)
                .baseShoppingScore(0.7f)
                .publicTransportScore(0.7f)
                .walkabilityScore(0.65f)
                .description("Capital of Ukraine")
                .imageUrl("https://example.com/kyiv.jpg")
                .build();
    }

    private City buildCity() {
        return City.builder()
                .id(1L)
                .country(country)
                .name("Kyiv")
                .region("Central Ukraine")
                .cityType(CityType.MEGAPOLIS)
                .population(2950000)
                .climateType(ClimateType.CONTINENTAL)
                .avgTempSummer(25.0f)
                .avgTempWinter(-3.0f)
                .latitude(50.4501)
                .longitude(30.5234)
                .baseCostLevel(0.35f)
                .baseSafetyScore(0.6f)
                .baseCultureScore(0.85f)
                .baseFoodScore(0.75f)
                .baseNightlifeScore(0.8f)
                .baseNatureScore(0.5f)
                .baseBeachScore(0.1f)
                .baseArchitectureScore(0.8f)
                .baseShoppingScore(0.7f)
                .costLevel(0.35f)
                .safetyScore(0.6f)
                .cultureScore(0.85f)
                .foodScore(0.75f)
                .nightlifeScore(0.8f)
                .natureScore(0.5f)
                .beachScore(0.1f)
                .architectureScore(0.8f)
                .shoppingScore(0.7f)
                .publicTransportScore(0.7f)
                .walkabilityScore(0.65f)
                .popularity(0.5f)
                .ratingCount(42)
                .description("Capital of Ukraine")
                .imageUrl("https://example.com/kyiv.jpg")
                .build();
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from request")
        void shouldMapAllFields() {
            CityRequest request = buildRequest();
            City city = mapper.toEntity(request, country);

            assertEquals("Kyiv", city.getName());
            assertEquals("Central Ukraine", city.getRegion());
            assertEquals(CityType.MEGAPOLIS, city.getCityType());
            assertEquals(2950000, city.getPopulation());
            assertEquals(ClimateType.CONTINENTAL, city.getClimateType());
            assertEquals(25.0f, city.getAvgTempSummer());
            assertEquals(-3.0f, city.getAvgTempWinter());
            assertEquals(50.4501, city.getLatitude(), 0.0001);
            assertEquals(30.5234, city.getLongitude(), 0.0001);
        }

        @Test
        @DisplayName("should set country reference")
        void shouldSetCountry() {
            CityRequest request = buildRequest();
            City city = mapper.toEntity(request, country);

            assertEquals(country, city.getCountry());
            assertEquals(1L, city.getCountry().getId());
        }

        @Test
        @DisplayName("should map all base scores")
        void shouldMapBaseScores() {
            CityRequest request = buildRequest();
            City city = mapper.toEntity(request, country);

            assertEquals(0.35f, city.getBaseCostLevel());
            assertEquals(0.6f, city.getBaseSafetyScore());
            assertEquals(0.85f, city.getBaseCultureScore());
            assertEquals(0.75f, city.getBaseFoodScore());
            assertEquals(0.8f, city.getBaseNightlifeScore());
            assertEquals(0.5f, city.getBaseNatureScore());
            assertEquals(0.1f, city.getBaseBeachScore());
            assertEquals(0.8f, city.getBaseArchitectureScore());
            assertEquals(0.7f, city.getBaseShoppingScore());
        }

        @Test
        @DisplayName("should map static scores")
        void shouldMapStaticScores() {
            CityRequest request = buildRequest();
            City city = mapper.toEntity(request, country);

            assertEquals(0.7f, city.getPublicTransportScore());
            assertEquals(0.65f, city.getWalkabilityScore());
        }

        @Test
        @DisplayName("should not set id")
        void shouldNotSetId() {
            CityRequest request = buildRequest();
            City city = mapper.toEntity(request, country);

            assertNull(city.getId());
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all fields to response")
        void shouldMapAllFields() {
            City city = buildCity();
            CityResponse response = mapper.toResponse(city);

            assertEquals(1L, response.getId());
            assertEquals("Kyiv", response.getName());
            assertEquals("Central Ukraine", response.getRegion());
            assertEquals(CityType.MEGAPOLIS, response.getCityType());
            assertEquals(2950000, response.getPopulation());
            assertEquals(ClimateType.CONTINENTAL, response.getClimateType());
            assertEquals(25.0f, response.getAvgTempSummer());
            assertEquals(-3.0f, response.getAvgTempWinter());
            assertEquals(50.4501, response.getLatitude(), 0.0001);
            assertEquals(30.5234, response.getLongitude(), 0.0001);
        }

        @Test
        @DisplayName("should map calculated scores not base scores")
        void shouldMapCalculatedScores() {
            City city = buildCity();
            CityResponse response = mapper.toResponse(city);

            assertEquals(0.85f, response.getCultureScore());
            assertEquals(0.75f, response.getFoodScore());
            assertEquals(0.8f, response.getNightlifeScore());
            assertEquals(0.5f, response.getNatureScore());
            assertEquals(0.6f, response.getSafetyScore());
            assertEquals(0.35f, response.getCostLevel());
            assertEquals(0.1f, response.getBeachScore());
            assertEquals(0.8f, response.getArchitectureScore());
            assertEquals(0.7f, response.getShoppingScore());
        }

        @Test
        @DisplayName("should map popularity and rating count")
        void shouldMapPopularityAndRatingCount() {
            City city = buildCity();
            CityResponse response = mapper.toResponse(city);

            assertEquals(0.5f, response.getPopularity());
            assertEquals(42, response.getRatingCount());
        }

        @Test
        @DisplayName("should flatten country info")
        void shouldFlattenCountryInfo() {
            City city = buildCity();
            CityResponse response = mapper.toResponse(city);

            assertEquals(1L, response.getCountryId());
            assertEquals("Ukraine", response.getCountryName());
            assertEquals("UA", response.getCountryCode());
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            City city = buildCity();
            CityRequest request = CityRequest.builder()
                    .name("Lviv")
                    .region("Western Ukraine")
                    .cityType(CityType.LARGE_CITY)
                    .population(720000)
                    .climateType(ClimateType.CONTINENTAL)
                    .avgTempSummer(22.0f)
                    .avgTempWinter(-2.0f)
                    .latitude(49.8397)
                    .longitude(24.0297)
                    .baseCostLevel(0.3f)
                    .baseSafetyScore(0.75f)
                    .baseCultureScore(0.9f)
                    .baseFoodScore(0.85f)
                    .baseNightlifeScore(0.6f)
                    .baseNatureScore(0.65f)
                    .baseBeachScore(0.05f)
                    .baseArchitectureScore(0.95f)
                    .baseShoppingScore(0.5f)
                    .publicTransportScore(0.55f)
                    .walkabilityScore(0.8f)
                    .description("Cultural capital")
                    .imageUrl("https://example.com/lviv.jpg")
                    .build();

            mapper.updateEntity(city, request);

            assertEquals("Lviv", city.getName());
            assertEquals("Western Ukraine", city.getRegion());
            assertEquals(CityType.LARGE_CITY, city.getCityType());
            assertEquals(720000, city.getPopulation());
            assertEquals(49.8397, city.getLatitude(), 0.0001);
            assertEquals(24.0297, city.getLongitude(), 0.0001);
            assertEquals(0.9f, city.getBaseCultureScore());
            assertEquals(0.55f, city.getPublicTransportScore());
            assertEquals(0.8f, city.getWalkabilityScore());
        }

        @Test
        @DisplayName("should preserve id after update")
        void shouldPreserveId() {
            City city = buildCity();
            CityRequest request = buildRequest();
            request.setName("Updated");

            mapper.updateEntity(city, request);

            assertEquals(1L, city.getId());
        }

        @Test
        @DisplayName("should not change calculated scores")
        void shouldNotChangeCalculatedScores() {
            City city = buildCity();
            city.setCultureScore(0.92f);
            city.setFoodScore(0.88f);

            CityRequest request = buildRequest();
            mapper.updateEntity(city, request);

            assertEquals(0.92f, city.getCultureScore());
            assertEquals(0.88f, city.getFoodScore());
        }
    }
}
