package com.travelRec.service;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.CityMapper;
import com.travelRec.repository.CityRepository;
import com.travelRec.repository.UserPreferencesRepository;
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
class RecommendationServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Spy
    private CityMapper cityMapper = new CityMapper();

    @InjectMocks
    private RecommendationService recommendationService;

    private Country country;
    private UserPreferences prefs;

    @BeforeEach
    void setUp() {
        country = Country.builder()
                .id(1L)
                .name("Hungary")
                .code("HU")
                .continent(Continent.EUROPE)
                .build();

        prefs = UserPreferences.builder()
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
                .build();
    }

    private City buildCity(String name, float culture, float food, float nightlife,
                           float nature, float safety, float cost,
                           float beach, float architecture, float shopping) {
        return City.builder()
                .id((long) name.hashCode())
                .country(country)
                .name(name)
                .cityType(CityType.LARGE_CITY)
                .climateType(ClimateType.TEMPERATE)
                .latitude(47.5)
                .longitude(19.0)
                .baseCultureScore(culture).baseFoodScore(food).baseNightlifeScore(nightlife)
                .baseNatureScore(nature).baseSafetyScore(safety).baseCostLevel(cost)
                .baseBeachScore(beach).baseArchitectureScore(architecture).baseShoppingScore(shopping)
                .cultureScore(culture).foodScore(food).nightlifeScore(nightlife)
                .natureScore(nature).safetyScore(safety).costLevel(cost)
                .beachScore(beach).architectureScore(architecture).shoppingScore(shopping)
                .publicTransportScore(0.5f).walkabilityScore(0.5f)
                .popularity(0.5f).ratingCount(0)
                .build();
    }

    @Nested
    @DisplayName("cosineSimilarity()")
    class CosineSimilarity {

        @Test
        @DisplayName("should return 1.0 for identical vectors")
        void shouldReturnOneForIdentical() {
            double[] a = {0.8, 0.6, 0.9, 0.3, 0.5, 0.7, 0.2, 0.4, 0.1};
            double score = RecommendationService.cosineSimilarity(a, a);
            assertEquals(1.0, score, 0.001);
        }

        @Test
        @DisplayName("should return 0.0 for orthogonal vectors")
        void shouldReturnZeroForOrthogonal() {
            double[] a = {1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            double[] b = {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            double score = RecommendationService.cosineSimilarity(a, b);
            assertEquals(0.0, score, 0.001);
        }

        @Test
        @DisplayName("should return 0.0 for zero vector")
        void shouldReturnZeroForZeroVector() {
            double[] a = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
            double[] b = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
            double score = RecommendationService.cosineSimilarity(a, b);
            assertEquals(0.0, score, 0.001);
        }

        @Test
        @DisplayName("should return higher score for more similar vectors")
        void shouldReturnHigherForSimilar() {
            double[] user = {0.9, 0.8, 0.3, 0.7, 0.2, 0.6, 0.4, 0.5, 0.1};
            double[] budapest = {0.85, 0.80, 0.40, 0.60, 0.30, 0.35, 0.15, 0.60, 0.50};
            double[] zurich = {0.75, 0.70, 0.40, 0.85, 0.95, 0.90, 0.10, 0.80, 0.60};

            double scoreBudapest = RecommendationService.cosineSimilarity(user, budapest);
            double scoreZurich = RecommendationService.cosineSimilarity(user, zurich);

            assertTrue(scoreBudapest > scoreZurich);
        }

        @Test
        @DisplayName("should be symmetric")
        void shouldBeSymmetric() {
            double[] a = {0.9, 0.8, 0.3, 0.7, 0.2, 0.6, 0.4, 0.5, 0.1};
            double[] b = {0.5, 0.6, 0.7, 0.3, 0.8, 0.4, 0.9, 0.2, 0.1};
            assertEquals(
                    RecommendationService.cosineSimilarity(a, b),
                    RecommendationService.cosineSimilarity(b, a),
                    0.0001
            );
        }

        @Test
        @DisplayName("should throw for different length vectors")
        void shouldThrowForDifferentLengths() {
            double[] a = {0.5, 0.5};
            double[] b = {0.5, 0.5, 0.5};
            assertThrows(IllegalArgumentException.class,
                    () -> RecommendationService.cosineSimilarity(a, b));
        }

        @Test
        @DisplayName("should return value between 0 and 1 for positive vectors")
        void shouldReturnBetweenZeroAndOne() {
            double[] a = {0.1, 0.9, 0.5, 0.3, 0.7, 0.2, 0.8, 0.4, 0.6};
            double[] b = {0.6, 0.2, 0.8, 0.4, 0.1, 0.9, 0.3, 0.7, 0.5};
            double score = RecommendationService.cosineSimilarity(a, b);
            assertTrue(score >= 0.0 && score <= 1.0);
        }
    }

    @Nested
    @DisplayName("haversineDistance()")
    class HaversineDistance {

        @Test
        @DisplayName("should return 0 for same point")
        void shouldReturnZeroForSamePoint() {
            double distance = RecommendationService.haversineDistance(50.45, 30.52, 50.45, 30.52);
            assertEquals(0.0, distance, 0.001);
        }

        @Test
        @DisplayName("should calculate Kyiv to Lviv correctly (~470 km)")
        void shouldCalculateKyivToLviv() {
            double distance = RecommendationService.haversineDistance(50.4501, 30.5234, 49.8397, 24.0297);
            assertTrue(distance > 450 && distance < 490);
        }

        @Test
        @DisplayName("should calculate Budapest to Vienna correctly (~215 km)")
        void shouldCalculateBudapestToVienna() {
            double distance = RecommendationService.haversineDistance(47.4979, 19.0402, 48.2082, 16.3738);
            assertTrue(distance > 200 && distance < 240);
        }

        @Test
        @DisplayName("should be symmetric")
        void shouldBeSymmetric() {
            double d1 = RecommendationService.haversineDistance(50.45, 30.52, 48.21, 16.37);
            double d2 = RecommendationService.haversineDistance(48.21, 16.37, 50.45, 30.52);
            assertEquals(d1, d2, 0.001);
        }
    }

    @Nested
    @DisplayName("getPersonalized()")
    class GetPersonalized {

        @Test
        @DisplayName("should return top N cities sorted by similarity")
        void shouldReturnTopN() {
            City highMatch = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            City medMatch = buildCity("Vienna", 0.6f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
            City lowMatch = buildCity("Zurich", 0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.1f, 0.9f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAll()).thenReturn(List.of(highMatch, medMatch, lowMatch));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 2, null, null, null);

            assertEquals(2, results.size());
            assertTrue(results.get(0).getSimilarityScore() >= results.get(1).getSimilarityScore());
        }

        @Test
        @DisplayName("should filter by continent")
        void shouldFilterByContinent() {
            Country asiaCountry = Country.builder().id(2L).name("Japan").code("JP").continent(Continent.ASIA).build();
            City europeanCity = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            City asianCity = buildCity("Tokyo", 0.9f, 0.9f, 0.9f, 0.3f, 0.9f, 0.7f, 0.1f, 0.8f, 0.9f);
            asianCity.setCountry(asiaCountry);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAll()).thenReturn(List.of(europeanCity, asianCity));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, Continent.EUROPE, null, null);

            assertEquals(1, results.size());
            assertEquals("Budapest", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should filter by city type")
        void shouldFilterByCityType() {
            City megapolis = buildCity("Tokyo", 0.9f, 0.9f, 0.9f, 0.3f, 0.9f, 0.7f, 0.1f, 0.8f, 0.9f);
            megapolis.setCityType(CityType.MEGAPOLIS);
            City resort = buildCity("Antalya", 0.4f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            resort.setCityType(CityType.RESORT);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAll()).thenReturn(List.of(megapolis, resort));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, null, CityType.RESORT, null);

            assertEquals(1, results.size());
            assertEquals("Antalya", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should return empty list when no cities match filters")
        void shouldReturnEmptyWhenNoMatch() {
            City city = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAll()).thenReturn(List.of(city));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, Continent.ASIA, null, null);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should use preferred filters from preferences when not specified")
        void shouldUsePreferredFilters() {
            prefs.setPreferredCityType(CityType.RESORT);
            City resort = buildCity("Antalya", 0.5f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            resort.setCityType(CityType.RESORT);
            City large = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            large.setCityType(CityType.LARGE_CITY);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAll()).thenReturn(List.of(resort, large));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, null, null, null);

            assertEquals(1, results.size());
            assertEquals("Antalya", results.get(0).getCity().getName());
        }
    }

    @Nested
    @DisplayName("updatePreferences()")
    class UpdatePreferences {

        @Test
        @DisplayName("should increase weight when rating exceeds expectation")
        void shouldIncreaseWeight() {
            City city = buildCity("Istanbul", 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(5)
                    .foodRating(5)
                    .build();

            float oldWeight = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertTrue(prefs.getFoodWeight() > oldWeight);
        }

        @Test
        @DisplayName("should decrease weight when rating below expectation")
        void shouldDecreaseWeight() {
            City city = buildCity("Istanbul", 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(2)
                    .foodRating(1)
                    .build();

            float oldWeight = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertTrue(prefs.getFoodWeight() < oldWeight);
        }

        @Test
        @DisplayName("should not update weights for quick rating")
        void shouldNotUpdateForQuickRating() {
            City city = buildCity("Istanbul", 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(5)
                    .build();

            float oldCulture = prefs.getCultureWeight();
            float oldFood = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertEquals(oldCulture, prefs.getCultureWeight());
            assertEquals(oldFood, prefs.getFoodWeight());
        }

        @Test
        @DisplayName("should only update rated categories")
        void shouldOnlyUpdateRatedCategories() {
            City city = buildCity("Istanbul", 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(4)
                    .cultureRating(5)
                    .build();

            float oldFood = prefs.getFoodWeight();
            float oldNightlife = prefs.getNightlifeWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertEquals(oldFood, prefs.getFoodWeight());
            assertEquals(oldNightlife, prefs.getNightlifeWeight());
        }

        @Test
        @DisplayName("should clamp weight between 0 and 1")
        void shouldClampWeight() {
            prefs.setCultureWeight(0.99f);
            City city = buildCity("Florence", 0.95f, 0.8f, 0.3f, 0.7f, 0.8f, 0.6f, 0.2f, 0.95f, 0.4f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(5)
                    .cultureRating(5)
                    .build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertTrue(prefs.getCultureWeight() <= 1.0f);
            assertTrue(prefs.getCultureWeight() >= 0.0f);
        }

        @Test
        @DisplayName("should calculate and update preferences weights based on new rating")
        void shouldUpdatePreferences() {
            City city = buildCity("Istanbul", 0.8f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            Rating rating = Rating.builder()
                    .user(User.builder().id(1L).build())
                    .city(city)
                    .overallScore(4)
                    .cultureRating(5)
                    .build();

            float oldCultureWeight = 0.5f;
            prefs.setCultureWeight(oldCultureWeight);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(rating.getUser(), rating);

            assertNotEquals(oldCultureWeight, prefs.getCultureWeight(),
                    "Culture weight should have been updated");

            verify(preferencesRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPopular()")
    class GetPopular {

        @Test
        @DisplayName("should return cities without similarity score")
        void shouldReturnWithoutSimilarity() {
            City city = buildCity("Paris", 0.9f, 0.9f, 0.8f, 0.5f, 0.7f, 0.8f, 0.1f, 0.95f, 0.9f);
            when(cityRepository.findTopByPopularity(5)).thenReturn(List.of(city));

            List<RecommendationResponse> results = recommendationService.getPopular(5);

            assertEquals(1, results.size());
            assertNull(results.get(0).getSimilarityScore());
            assertEquals("Paris", results.get(0).getCity().getName());
        }
    }
}
