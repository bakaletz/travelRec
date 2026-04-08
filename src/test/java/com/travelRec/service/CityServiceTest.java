package com.travelRec.service;

import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.CityMapper;
import com.travelRec.repository.CityRepository;
import com.travelRec.repository.RatingRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private CountryService countryService;

    @Spy
    private CityMapper cityMapper = new CityMapper();

    @InjectMocks
    private CityService cityService;

    private Country country;
    private City city;

    @BeforeEach
    void setUp() {
        country = Country.builder()
                .id(1L).name("Hungary").code("HU").continent(Continent.EUROPE).build();

        city = City.builder()
                .id(1L).country(country).name("Budapest")
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .latitude(47.5).longitude(19.0)
                .baseCultureScore(0.85f).baseFoodScore(0.8f).baseNightlifeScore(0.9f)
                .baseNatureScore(0.6f).baseSafetyScore(0.7f).baseCostLevel(0.35f)
                .baseBeachScore(0.15f).baseArchitectureScore(0.9f).baseShoppingScore(0.5f)
                .cultureScore(0.85f).foodScore(0.8f).nightlifeScore(0.9f)
                .natureScore(0.6f).safetyScore(0.7f).costLevel(0.35f)
                .beachScore(0.15f).architectureScore(0.9f).shoppingScore(0.5f)
                .publicTransportScore(0.8f).walkabilityScore(0.7f)
                .popularity(0.75f).ratingCount(0)
                .build();
    }

    @Nested
    @DisplayName("getCityById()")
    class GetCityById {

        @Test
        @DisplayName("should return city response")
        void shouldReturnCity() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            CityResponse response = cityService.getCityById(1L);

            assertEquals("Budapest", response.getName());
            assertEquals("Hungary", response.getCountryName());
        }

        @Test
        @DisplayName("should throw when city not found")
        void shouldThrowWhenNotFound() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.getCityById(99L));
        }
    }

    @Nested
    @DisplayName("getNearbyCities()")
    class GetNearbyCities {

        @Test
        @DisplayName("should call repository with correct coordinates")
        void shouldCallWithCoordinates() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(cityRepository.findNearbyCities(1L, 47.5, 19.0, 300.0)).thenReturn(List.of());

            cityService.getNearbyCities(1L, 300.0);

            verify(cityRepository).findNearbyCities(1L, 47.5, 19.0, 300.0);
        }

        @Test
        @DisplayName("should throw when origin city not found")
        void shouldThrowWhenOriginNotFound() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.getNearbyCities(99L, 300.0));
        }
    }

    @Nested
    @DisplayName("recalculateScores()")
    class RecalculateScores {

        @Test
        @DisplayName("should not change scores when no ratings exist")
        void shouldNotChangeWhenNoRatings() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(ratingRepository.countByCityId(1L)).thenReturn(0L);

            cityService.recalculateScores(1L);

            assertEquals(0.85f, city.getCultureScore());
            verify(cityRepository, never()).save(any());
        }

        @Test
        @DisplayName("should blend base and user scores when ratings exist")
        void shouldBlendScores() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(ratingRepository.countByCityId(1L)).thenReturn(20L);
            when(ratingRepository.avgCultureRatingByCityId(1L)).thenReturn(Optional.of(4.5));
            when(ratingRepository.avgFoodRatingByCityId(1L)).thenReturn(Optional.of(3.0));
            when(ratingRepository.avgNightlifeRatingByCityId(1L)).thenReturn(Optional.of(5.0));
            when(ratingRepository.avgNatureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgSafetyRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgCostRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgBeachRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgArchitectureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgShoppingRatingByCityId(1L)).thenReturn(Optional.empty());

            cityService.recalculateScores(1L);

            assertNotEquals(0.85f, city.getCultureScore());
            assertEquals(20, city.getRatingCount());
            verify(cityRepository).save(city);
        }

        @Test
        @DisplayName("should keep base score when category has no ratings")
        void shouldKeepBaseWhenNoCategoryRatings() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(ratingRepository.countByCityId(1L)).thenReturn(5L);
            when(ratingRepository.avgCultureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgFoodRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNightlifeRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNatureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgSafetyRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgCostRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgBeachRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgArchitectureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgShoppingRatingByCityId(1L)).thenReturn(Optional.empty());

            cityService.recalculateScores(1L);

            assertEquals(0.85f, city.getCultureScore());
            assertEquals(0.8f, city.getFoodScore());
        }

        @Test
        @DisplayName("should increase user weight with more ratings")
        void shouldIncreaseUserWeight() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(ratingRepository.countByCityId(1L)).thenReturn(100L);
            when(ratingRepository.avgCultureRatingByCityId(1L)).thenReturn(Optional.of(2.0));
            when(ratingRepository.avgFoodRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNightlifeRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNatureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgSafetyRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgCostRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgBeachRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgArchitectureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgShoppingRatingByCityId(1L)).thenReturn(Optional.empty());

            float scoreBefore = city.getCultureScore();
            cityService.recalculateScores(1L);
            float scoreAfter100 = city.getCultureScore();

            assertTrue(scoreAfter100 < scoreBefore);
        }
    }

    @Nested
    @DisplayName("createCity()")
    class CreateCity {

        @Test
        @DisplayName("should create and return city response")
        void shouldCreateCity() {
            CityRequest request = CityRequest.builder()
                    .countryId(1L).name("Vienna")
                    .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                    .latitude(48.2).longitude(16.37)
                    .baseCultureScore(0.9f).baseFoodScore(0.7f).baseNightlifeScore(0.6f)
                    .baseNatureScore(0.7f).baseSafetyScore(0.9f).baseCostLevel(0.7f)
                    .baseBeachScore(0.05f).baseArchitectureScore(0.95f).baseShoppingScore(0.7f)
                    .publicTransportScore(0.9f).walkabilityScore(0.85f)
                    .build();

            when(countryService.findCountryOrThrow(1L)).thenReturn(country);
            when(cityRepository.save(any(City.class))).thenAnswer(inv -> {
                City c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            CityResponse response = cityService.createCity(request);

            assertEquals("Vienna", response.getName());
            verify(cityRepository).save(any(City.class));
        }
    }

    @Nested
    @DisplayName("deleteCity()")
    class DeleteCity {

        @Test
        @DisplayName("should delete existing city")
        void shouldDelete() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            cityService.deleteCity(1L);

            verify(cityRepository).delete(city);
        }

        @Test
        @DisplayName("should throw when city not found")
        void shouldThrowWhenNotFound() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.deleteCity(99L));
        }
    }
}
