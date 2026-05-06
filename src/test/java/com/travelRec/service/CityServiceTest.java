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
                .id(1L).country(country).name("Budapest").region("Central Hungary")
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
    @DisplayName("getAllCities()")
    class GetAllCities {

        @Test
        @DisplayName("should return all cities")
        void shouldReturnAll() {
            when(cityRepository.findAll()).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getAllCities();

            assertEquals(1, result.size());
            assertEquals("Budapest", result.get(0).getName());
        }

        @Test
        @DisplayName("should return empty list")
        void shouldReturnEmpty() {
            when(cityRepository.findAll()).thenReturn(List.of());

            assertTrue(cityService.getAllCities().isEmpty());
        }
    }

    @Nested
    @DisplayName("getCityById()")
    class GetCityById {

        @Test
        @DisplayName("should return city")
        void shouldReturn() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            CityResponse response = cityService.getCityById(1L);

            assertEquals("Budapest", response.getName());
            assertEquals("Hungary", response.getCountryName());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.getCityById(99L));
        }
    }

    @Nested
    @DisplayName("getCitiesByCountry()")
    class GetCitiesByCountry {

        @Test
        @DisplayName("should return cities for country")
        void shouldReturn() {
            when(cityRepository.findByCountryId(1L)).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getCitiesByCountry(1L);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getCitiesByContinent()")
    class GetCitiesByContinent {

        @Test
        @DisplayName("should return cities for continent")
        void shouldReturn() {
            when(cityRepository.findByCountryContinent(Continent.EUROPE)).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getCitiesByContinent(Continent.EUROPE);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should return empty for unknown continent")
        void shouldReturnEmpty() {
            when(cityRepository.findByCountryContinent(Continent.ANTARCTICA)).thenReturn(List.of());

            assertTrue(cityService.getCitiesByContinent(Continent.ANTARCTICA).isEmpty());
        }
    }

    @Nested
    @DisplayName("getCitiesByCityType()")
    class GetCitiesByCityType {

        @Test
        @DisplayName("should return cities for type")
        void shouldReturn() {
            when(cityRepository.findByCityType(CityType.LARGE_CITY)).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getCitiesByCityType(CityType.LARGE_CITY);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("getCitiesByClimateType()")
    class GetCitiesByClimateType {

        @Test
        @DisplayName("should return cities for climate")
        void shouldReturn() {
            when(cityRepository.findByClimateType(ClimateType.CONTINENTAL)).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getCitiesByClimateType(ClimateType.CONTINENTAL);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("searchCities()")
    class SearchCities {

        @Test
        @DisplayName("should return cities matching query")
        void shouldReturn() {
            when(cityRepository.searchByNameOrRegion("buda")).thenReturn(List.of(city));

            List<CityResponse> result = cityService.searchCities("buda");

            assertEquals(1, result.size());
            assertEquals("Budapest", result.get(0).getName());
        }

        @Test
        @DisplayName("should return empty for non-matching query")
        void shouldReturnEmpty() {
            when(cityRepository.searchByNameOrRegion("xyz")).thenReturn(List.of());

            assertTrue(cityService.searchCities("xyz").isEmpty());
        }
    }

    @Nested
    @DisplayName("getPopularCities()")
    class GetPopularCities {

        @Test
        @DisplayName("should return top popular cities")
        void shouldReturn() {
            when(cityRepository.findTopByPopularity(5)).thenReturn(List.of(city));

            List<CityResponse> result = cityService.getPopularCities(5);

            assertEquals(1, result.size());
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
    @DisplayName("getCitiesInSameCountry()")
    class GetCitiesInSameCountry {

        @Test
        @DisplayName("should return other cities of same country")
        void shouldReturn() {
            City other = City.builder()
                    .id(2L).country(country).name("Debrecen")
                    .cityType(CityType.MEDIUM_CITY).climateType(ClimateType.CONTINENTAL)
                    .build();

            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(cityRepository.findByCountryIdExcluding(1L, 1L)).thenReturn(List.of(other));

            List<CityResponse> result = cityService.getCitiesInSameCountry(1L);

            assertEquals(1, result.size());
            assertEquals("Debrecen", result.get(0).getName());
        }

        @Test
        @DisplayName("should throw when origin city not found")
        void shouldThrow() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.getCitiesInSameCountry(99L));
        }
    }

    @Nested
    @DisplayName("createCity()")
    class CreateCity {

        @Test
        @DisplayName("should create and return city")
        void shouldCreate() {
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
    @DisplayName("updateCity()")
    class UpdateCity {

        @Test
        @DisplayName("should update city via dirty checking")
        void shouldUpdate() {
            CityRequest request = CityRequest.builder()
                    .name("Buda")
                    .cityType(CityType.MEDIUM_CITY)
                    .climateType(ClimateType.CONTINENTAL)
                    .latitude(47.5).longitude(19.0)
                    .baseCultureScore(0.85f).baseFoodScore(0.8f).baseNightlifeScore(0.9f)
                    .baseNatureScore(0.6f).baseSafetyScore(0.7f).baseCostLevel(0.35f)
                    .baseBeachScore(0.15f).baseArchitectureScore(0.9f).baseShoppingScore(0.5f)
                    .publicTransportScore(0.8f).walkabilityScore(0.7f)
                    .build();

            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            CityResponse response = cityService.updateCity(1L, request);

            assertEquals("Buda", response.getName());
            assertEquals(CityType.MEDIUM_CITY, city.getCityType());
            verify(cityRepository, never()).save(any(City.class));
        }

        @Test
        @DisplayName("should throw when city not found")
        void shouldThrow() {
            CityRequest request = CityRequest.builder().name("X").build();
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.updateCity(99L, request));
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
        @DisplayName("should cap user weight at 0.6 with many ratings")
        void shouldCapUserWeight() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
            when(ratingRepository.countByCityId(1L)).thenReturn(1000L);
            when(ratingRepository.avgCultureRatingByCityId(1L)).thenReturn(Optional.of(1.0));
            when(ratingRepository.avgFoodRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNightlifeRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgNatureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgSafetyRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgCostRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgBeachRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgArchitectureRatingByCityId(1L)).thenReturn(Optional.empty());
            when(ratingRepository.avgShoppingRatingByCityId(1L)).thenReturn(Optional.empty());

            cityService.recalculateScores(1L);

            // base=0.85 with weight 0.4, normalized rating=0.0 with weight 0.6 -> 0.34
            // not 0.0 because base still has 40% weight (cap)
            assertTrue(city.getCultureScore() > 0.3f && city.getCultureScore() < 0.4f);
        }
    }

    @Nested
    @DisplayName("incrementPopularity()")
    class IncrementPopularity {

        @Test
        @DisplayName("should increase popularity by 1.0")
        void shouldIncrement() {
            float oldPopularity = city.getPopularity();
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            cityService.incrementPopularity(1L);

            assertEquals(oldPopularity + 1.0f, city.getPopularity());
        }

        @Test
        @DisplayName("should throw when city not found")
        void shouldThrow() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.incrementPopularity(99L));
        }
    }

    @Nested
    @DisplayName("decrementPopularity()")
    class DecrementPopularity {

        @Test
        @DisplayName("should decrease popularity by 1.0")
        void shouldDecrement() {
            city.setPopularity(5.0f);
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            cityService.decrementPopularity(1L);

            assertEquals(4.0f, city.getPopularity());
        }

        @Test
        @DisplayName("should not go below zero")
        void shouldNotGoBelowZero() {
            city.setPopularity(0.5f);
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            cityService.decrementPopularity(1L);

            assertEquals(0.0f, city.getPopularity());
        }
    }

    @Nested
    @DisplayName("createCity() and deleteCity()")
    class CreateAndDelete {

        @Test
        @DisplayName("should delete existing city")
        void shouldDelete() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            cityService.deleteCity(1L);

            verify(cityRepository).delete(city);
        }

        @Test
        @DisplayName("should throw when city not found on delete")
        void shouldThrowOnDelete() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.deleteCity(99L));
        }
    }

    @Nested
    @DisplayName("findCityOrThrow()")
    class FindCityOrThrow {

        @Test
        @DisplayName("should return city when found")
        void shouldReturn() {
            when(cityRepository.findById(1L)).thenReturn(Optional.of(city));

            City found = cityService.findCityOrThrow(1L);

            assertEquals(city, found);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> cityService.findCityOrThrow(99L));
        }
    }
}