package com.travelRec.service;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.CityMapper;
import com.travelRec.repository.CityRepository;
import com.travelRec.repository.RatingRepository;
import com.travelRec.repository.UserPreferencesRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private UserPreferencesRepository preferencesRepository;

    @Mock
    private RatingRepository ratingRepository;

    @Spy
    private CityMapper cityMapper = new CityMapper();

    @InjectMocks
    private RecommendationService recommendationService;

    private Country country;
    private UserPreferences prefs;
    private User user;

    @BeforeEach
    void setUp() {
        country = Country.builder()
                .id(1L)
                .name("Hungary")
                .code("HU")
                .continent(Continent.EUROPE)
                .build();

        user = User.builder().id(1L).email("anna@mail.com").firstName("Anna").lastName("S").role(Role.USER).build();

        prefs = UserPreferences.builder()
                .id(1L)
                .user(user)
                .cultureWeight(0.9f)
                .foodWeight(0.8f)
                .nightlifeWeight(0.3f)
                .natureWeight(0.7f)
                .safetyWeight(0.2f)
                .budgetWeight(0.6f)
                .beachWeight(0.4f)
                .architectureWeight(0.5f)
                .shoppingWeight(0.1f)
                .cultureRatingCount(0)
                .foodRatingCount(0)
                .nightlifeRatingCount(0)
                .natureRatingCount(0)
                .safetyRatingCount(0)
                .budgetRatingCount(0)
                .beachRatingCount(0)
                .architectureRatingCount(0)
                .shoppingRatingCount(0)
                .build();
    }

    private City buildCity(String name, float culture, float food, float nightlife,
                           float nature, float safety, float cost,
                           float beach, float architecture, float shopping) {
        return City.builder()
                .id((long) Math.abs(name.hashCode()))
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
    @DisplayName("getPersonalized()")
    class GetPersonalized {

        @Test
        @DisplayName("should return cities sorted by similarity descending")
        void shouldSortByScore() {
            City highMatch = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            City medMatch = buildCity("Vienna", 0.6f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
            City lowMatch = buildCity("Zurich", 0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.1f, 0.9f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(highMatch, medMatch, lowMatch));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 3, null, null, null);

            assertEquals(3, results.size());
            assertTrue(results.get(0).getSimilarityScore() >= results.get(1).getSimilarityScore());
            assertTrue(results.get(1).getSimilarityScore() >= results.get(2).getSimilarityScore());
        }

        @Test
        @DisplayName("should respect limit parameter")
        void shouldRespectLimit() {
            City c1 = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            City c2 = buildCity("Vienna", 0.6f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
            City c3 = buildCity("Zurich", 0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.1f, 0.9f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(c1, c2, c3));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 2, null, null, null);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("should produce scores in [0, 1] range (centered cosine mapping)")
        void shouldProduceValidRange() {
            City c1 = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            City c2 = buildCity("Reykjavik", 0.1f, 0.1f, 0.9f, 0.9f, 0.9f, 0.1f, 0.0f, 0.5f, 0.0f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(c1, c2));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, null, null, null);

            for (RecommendationResponse r : results) {
                assertTrue(r.getSimilarityScore() >= 0.0 && r.getSimilarityScore() <= 1.0);
            }
        }

        @Test
        @DisplayName("should throw when preferences not found")
        void shouldThrowWhenPrefsNotFound() {
            when(preferencesRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> recommendationService.getPersonalized(99L, 10, null, null, null));
        }

        @Test
        @DisplayName("should filter by single continent")
        void shouldFilterBySingleContinent() {
            Country asiaCountry = Country.builder().id(2L).name("Japan").code("JP").continent(Continent.ASIA).build();
            City europeanCity = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            City asianCity = buildCity("Tokyo", 0.9f, 0.9f, 0.9f, 0.3f, 0.9f, 0.7f, 0.1f, 0.8f, 0.9f);
            asianCity.setCountry(asiaCountry);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(europeanCity, asianCity));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, List.of(Continent.EUROPE), null, null);

            assertEquals(1, results.size());
            assertEquals("Budapest", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should filter by multiple continents")
        void shouldFilterByMultipleContinents() {
            Country asiaCountry = Country.builder().id(2L).name("Japan").code("JP").continent(Continent.ASIA).build();
            Country africaCountry = Country.builder().id(3L).name("Kenya").code("KE").continent(Continent.AFRICA).build();

            City europeanCity = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            City asianCity = buildCity("Tokyo", 0.9f, 0.9f, 0.9f, 0.3f, 0.9f, 0.7f, 0.1f, 0.8f, 0.9f);
            asianCity.setCountry(asiaCountry);
            City africanCity = buildCity("Nairobi", 0.6f, 0.5f, 0.3f, 0.9f, 0.4f, 0.3f, 0.1f, 0.4f, 0.3f);
            africanCity.setCountry(africaCountry);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(europeanCity, asianCity, africanCity));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, List.of(Continent.EUROPE, Continent.ASIA), null, null);

            assertEquals(2, results.size());
            List<String> names = results.stream().map(r -> r.getCity().getName()).toList();
            assertTrue(names.contains("Budapest"));
            assertTrue(names.contains("Tokyo"));
            assertFalse(names.contains("Nairobi"));
        }

        @Test
        @DisplayName("should filter by single city type")
        void shouldFilterByCityType() {
            City megapolis = buildCity("Tokyo", 0.9f, 0.9f, 0.9f, 0.3f, 0.9f, 0.7f, 0.1f, 0.8f, 0.9f);
            megapolis.setCityType(CityType.MEGAPOLIS);
            City resort = buildCity("Antalya", 0.4f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            resort.setCityType(CityType.RESORT);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(megapolis, resort));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, null, List.of(CityType.RESORT), null);

            assertEquals(1, results.size());
            assertEquals("Antalya", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should filter by multiple climate types")
        void shouldFilterByMultipleClimateTypes() {
            City temperate = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            temperate.setClimateType(ClimateType.TEMPERATE);
            City mediterranean = buildCity("Barcelona", 0.9f, 0.9f, 0.8f, 0.5f, 0.6f, 0.6f, 0.8f, 0.9f, 0.8f);
            mediterranean.setClimateType(ClimateType.MEDITERRANEAN);
            City tropical = buildCity("Bangkok", 0.7f, 0.9f, 0.8f, 0.4f, 0.5f, 0.3f, 0.3f, 0.6f, 0.8f);
            tropical.setClimateType(ClimateType.TROPICAL);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(temperate, mediterranean, tropical));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, null, null, List.of(ClimateType.TEMPERATE, ClimateType.MEDITERRANEAN));

            assertEquals(2, results.size());
            List<String> names = results.stream().map(r -> r.getCity().getName()).toList();
            assertTrue(names.contains("Budapest"));
            assertTrue(names.contains("Barcelona"));
            assertFalse(names.contains("Bangkok"));
        }

        @Test
        @DisplayName("should combine multiple filter dimensions")
        void shouldCombineFilters() {
            Country asiaCountry = Country.builder().id(2L).name("Thailand").code("TH").continent(Continent.ASIA).build();

            City europeanResort = buildCity("Antalya", 0.4f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            europeanResort.setCityType(CityType.RESORT);
            europeanResort.setClimateType(ClimateType.MEDITERRANEAN);

            City europeanLarge = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            europeanLarge.setCityType(CityType.LARGE_CITY);
            europeanLarge.setClimateType(ClimateType.TEMPERATE);

            City asianResort = buildCity("Phuket", 0.3f, 0.8f, 0.7f, 0.9f, 0.6f, 0.3f, 0.95f, 0.2f, 0.4f);
            asianResort.setCountry(asiaCountry);
            asianResort.setCityType(CityType.RESORT);
            asianResort.setClimateType(ClimateType.TROPICAL);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(europeanResort, europeanLarge, asianResort));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, List.of(Continent.EUROPE), List.of(CityType.RESORT), null);

            assertEquals(1, results.size());
            assertEquals("Antalya", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should return empty list when no cities match filters")
        void shouldReturnEmptyWhenNoMatch() {
            City city = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(city));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, List.of(Continent.ASIA), null, null);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should return all when filter lists are empty")
        void shouldReturnAllWhenEmptyFilters() {
            City c1 = buildCity("Budapest", 0.8f, 0.8f, 0.8f, 0.5f, 0.7f, 0.4f, 0.2f, 0.9f, 0.5f);
            City c2 = buildCity("Vienna", 0.7f, 0.7f, 0.5f, 0.6f, 0.8f, 0.6f, 0.1f, 0.8f, 0.6f);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(c1, c2));

            List<RecommendationResponse> results = recommendationService.getPersonalized(
                    1L, 10, List.of(), List.of(), List.of());

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("should apply soft penalty when city does not match preferred city types")
        void shouldApplyCityTypePenalty() {
            prefs.setPreferredCityTypes(Set.of(CityType.RESORT));

            City matchingCity = buildCity("Antalya", 0.5f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            matchingCity.setCityType(CityType.RESORT);

            City nonMatchingCity = buildCity("Budapest", 0.5f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            nonMatchingCity.setCityType(CityType.LARGE_CITY);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(matchingCity, nonMatchingCity));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, null, null, null);

            assertEquals(2, results.size());
            // matching should rank higher because non-matching is multiplied by 0.85
            assertEquals("Antalya", results.get(0).getCity().getName());
            assertTrue(results.get(0).getSimilarityScore() > results.get(1).getSimilarityScore());
        }

        @Test
        @DisplayName("should apply soft penalty when city does not match preferred climate types")
        void shouldApplyClimatePenalty() {
            prefs.setPreferredClimateTypes(Set.of(ClimateType.TROPICAL));

            City matchingCity = buildCity("Bangkok", 0.5f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            matchingCity.setClimateType(ClimateType.TROPICAL);

            City nonMatchingCity = buildCity("Vienna", 0.5f, 0.7f, 0.6f, 0.8f, 0.7f, 0.4f, 0.95f, 0.3f, 0.5f);
            nonMatchingCity.setClimateType(ClimateType.CONTINENTAL);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(matchingCity, nonMatchingCity));

            List<RecommendationResponse> results = recommendationService.getPersonalized(1L, 10, null, null, null);

            assertEquals("Bangkok", results.get(0).getCity().getName());
            assertTrue(results.get(0).getSimilarityScore() > results.get(1).getSimilarityScore());
        }
    }

    @Nested
    @DisplayName("getPopular()")
    class GetPopular {

        @Test
        @DisplayName("should return cities without similarity score")
        void shouldReturnWithoutSimilarity() {
            City paris = buildCity("Paris", 0.9f, 0.9f, 0.8f, 0.5f, 0.7f, 0.8f, 0.1f, 0.95f, 0.9f);
            when(cityRepository.findTopByPopularity(5)).thenReturn(List.of(paris));

            List<RecommendationResponse> results = recommendationService.getPopular(5);

            assertEquals(1, results.size());
            assertNull(results.get(0).getSimilarityScore());
            assertEquals("Paris", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should return empty list when no popular cities")
        void shouldReturnEmpty() {
            when(cityRepository.findTopByPopularity(5)).thenReturn(List.of());

            List<RecommendationResponse> results = recommendationService.getPopular(5);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("getSimilarCities()")
    class GetSimilarCities {

        @Test
        @DisplayName("should return similar cities excluding the seed city itself")
        void shouldExcludeSeed() {
            City seed = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            seed.setId(1L);
            City c2 = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            c2.setId(2L);
            City c3 = buildCity("Prague", 0.8f, 0.7f, 0.5f, 0.6f, 0.35f, 0.5f, 0.4f, 0.5f, 0.2f);
            c3.setId(3L);

            when(cityRepository.findById(1L)).thenReturn(Optional.of(seed));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, c2, c3));

            List<RecommendationResponse> results = recommendationService.getSimilarCities(1L, 10);

            assertEquals(2, results.size());
            List<Long> ids = results.stream().map(r -> r.getCity().getId()).toList();
            assertFalse(ids.contains(1L));
        }

        @Test
        @DisplayName("should throw when seed city not found")
        void shouldThrowWhenSeedNotFound() {
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> recommendationService.getSimilarCities(99L, 10));
        }

        @Test
        @DisplayName("should not include reason in response")
        void shouldNotIncludeReason() {
            City seed = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            seed.setId(1L);
            City other = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            other.setId(2L);

            when(cityRepository.findById(1L)).thenReturn(Optional.of(seed));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, other));

            List<RecommendationResponse> results = recommendationService.getSimilarCities(1L, 10);

            assertNull(results.get(0).getReason());
        }

        @Test
        @DisplayName("should apply continuous context penalty for different city type")
        void shouldApplyCityTypePenalty() {
            // Use distinct (non-flat) vector so centered cosine is well-defined and identical between cities
            // (flat 0.5 vector centers to zero -> similarity is 0 -> penalty has no effect)
            City seed = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            seed.setId(1L);
            seed.setCityType(CityType.LARGE_CITY);

            City similarType = buildCity("Vienna", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            similarType.setId(2L);
            similarType.setCityType(CityType.LARGE_CITY);

            City veryDifferentType = buildCity("Hallstatt", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            veryDifferentType.setId(3L);
            veryDifferentType.setCityType(CityType.SMALL_TOWN);

            when(cityRepository.findById(1L)).thenReturn(Optional.of(seed));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, similarType, veryDifferentType));

            List<RecommendationResponse> results = recommendationService.getSimilarCities(1L, 10);

            // city of similar type should rank higher than the differently-typed one
            assertEquals("Vienna", results.get(0).getCity().getName());
            assertEquals("Hallstatt", results.get(1).getCity().getName());
            assertTrue(results.get(0).getSimilarityScore() > results.get(1).getSimilarityScore());
        }

        @Test
        @DisplayName("should apply continent penalty for cross-continent candidates")
        void shouldApplyContinentPenalty() {
            Country asiaCountry = Country.builder().id(2L).name("Japan").code("JP").continent(Continent.ASIA).build();

            City seed = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            seed.setId(1L);

            City sameContinent = buildCity("Vienna", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            sameContinent.setId(2L);

            City differentContinent = buildCity("Tokyo", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            differentContinent.setId(3L);
            differentContinent.setCountry(asiaCountry);

            when(cityRepository.findById(1L)).thenReturn(Optional.of(seed));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, sameContinent, differentContinent));

            List<RecommendationResponse> results = recommendationService.getSimilarCities(1L, 10);

            assertEquals("Vienna", results.get(0).getCity().getName());
            assertTrue(results.get(0).getSimilarityScore() > results.get(1).getSimilarityScore());
        }
    }

    @Nested
    @DisplayName("getBecauseYouLiked()")
    class GetBecauseYouLiked {

        @Test
        @DisplayName("should return empty list when no positive ratings")
        void shouldReturnEmptyWhenNoPositiveRatings() {
            when(ratingRepository.findRecentPositiveByUserIdWithCity(eq(1L), anyInt(), any(Pageable.class)))
                    .thenReturn(List.of());

            List<RecommendationResponse> results = recommendationService.getBecauseYouLiked(1L, 10);

            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("should exclude already-rated cities and seed")
        void shouldExcludeRatedCities() {
            City seed = buildCity("Istanbul", 0.85f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            seed.setId(1L);

            City c2 = buildCity("Athens", 0.8f, 0.85f, 0.7f, 0.5f, 0.6f, 0.5f, 0.4f, 0.85f, 0.5f);
            c2.setId(2L);
            City c3 = buildCity("Cairo", 0.85f, 0.7f, 0.5f, 0.4f, 0.5f, 0.5f, 0.4f, 0.7f, 0.5f);
            c3.setId(3L);

            Rating positiveRating = Rating.builder()
                    .id(10L).user(user).city(seed).overallScore(5).build();

            when(ratingRepository.findRecentPositiveByUserIdWithCity(eq(1L), anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(positiveRating));
            when(ratingRepository.findRatedCityIdsByUserId(1L)).thenReturn(List.of(1L, 3L));
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, c2, c3));

            List<RecommendationResponse> results = recommendationService.getBecauseYouLiked(1L, 10);

            assertEquals(1, results.size());
            assertEquals("Athens", results.get(0).getCity().getName());
        }

        @Test
        @DisplayName("should set reason field with seed city name")
        void shouldSetReason() {
            City seed = buildCity("Istanbul", 0.85f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            seed.setId(1L);
            City other = buildCity("Athens", 0.8f, 0.85f, 0.7f, 0.5f, 0.6f, 0.5f, 0.4f, 0.85f, 0.5f);
            other.setId(2L);

            Rating positiveRating = Rating.builder()
                    .id(10L).user(user).city(seed).overallScore(5).build();

            when(ratingRepository.findRecentPositiveByUserIdWithCity(eq(1L), anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(positiveRating));
            when(ratingRepository.findRatedCityIdsByUserId(1L)).thenReturn(List.of());
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seed, other));

            List<RecommendationResponse> results = recommendationService.getBecauseYouLiked(1L, 10);

            assertFalse(results.isEmpty());
            assertEquals("Because you liked Istanbul", results.get(0).getReason());
        }

        @Test
        @DisplayName("should pick seed by weighted random sampling from candidate pool")
        void shouldPickFromPool() {
            City seedA = buildCity("Istanbul", 0.85f, 0.9f, 0.7f, 0.5f, 0.6f, 0.4f, 0.3f, 0.7f, 0.5f);
            seedA.setId(1L);
            City seedB = buildCity("Rome", 0.9f, 0.85f, 0.7f, 0.5f, 0.65f, 0.5f, 0.4f, 0.95f, 0.5f);
            seedB.setId(2L);
            City other = buildCity("Athens", 0.8f, 0.85f, 0.7f, 0.5f, 0.6f, 0.5f, 0.4f, 0.85f, 0.5f);
            other.setId(3L);

            Rating r1 = Rating.builder().id(10L).user(user).city(seedA).overallScore(5).build();
            Rating r2 = Rating.builder().id(11L).user(user).city(seedB).overallScore(4).build();

            when(ratingRepository.findRecentPositiveByUserIdWithCity(eq(1L), anyInt(), any(Pageable.class)))
                    .thenReturn(List.of(r1, r2));
            when(ratingRepository.findRatedCityIdsByUserId(1L)).thenReturn(List.of());
            when(cityRepository.findAllWithCountry()).thenReturn(List.of(seedA, seedB, other));

            List<RecommendationResponse> results = recommendationService.getBecauseYouLiked(1L, 10);

            assertFalse(results.isEmpty());
            // reason should mention one of the seeds, not the recommended city
            String reason = results.get(0).getReason();
            assertTrue(reason.equals("Because you liked Istanbul") || reason.equals("Because you liked Rome"));
        }
    }

    @Nested
    @DisplayName("getNearbyRecommendations()")
    class GetNearbyRecommendations {

        @Test
        @DisplayName("should compute combined similarity and proximity score")
        void shouldComputeCombinedScore() {
            City origin = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            origin.setId(1L);
            origin.setLatitude(47.4979);
            origin.setLongitude(19.0402);

            City nearby = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            nearby.setId(2L);
            nearby.setLatitude(48.2082);
            nearby.setLongitude(16.3738);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findById(1L)).thenReturn(Optional.of(origin));
            when(cityRepository.findNearbyCities(eq(1L), anyDouble(), anyDouble(), eq(500.0)))
                    .thenReturn(List.of(nearby));

            List<RecommendationResponse> results = recommendationService.getNearbyRecommendations(1L, 1L, 500.0, 10);

            assertEquals(1, results.size());
            assertNotNull(results.get(0).getSimilarityScore());
            assertNotNull(results.get(0).getDistanceKm());
            // Budapest -> Vienna ≈ 215 km
            assertTrue(results.get(0).getDistanceKm() > 200 && results.get(0).getDistanceKm() < 240);
        }

        @Test
        @DisplayName("should throw when preferences not found")
        void shouldThrowWhenPrefsNotFound() {
            when(preferencesRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> recommendationService.getNearbyRecommendations(99L, 1L, 500.0, 10));
        }

        @Test
        @DisplayName("should throw when origin city not found")
        void shouldThrowWhenOriginNotFound() {
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> recommendationService.getNearbyRecommendations(1L, 99L, 500.0, 10));
        }

        @Test
        @DisplayName("should sort by combined score descending")
        void shouldSortByCombinedScore() {
            City origin = buildCity("Budapest", 0.9f, 0.8f, 0.3f, 0.7f, 0.2f, 0.6f, 0.4f, 0.5f, 0.1f);
            origin.setId(1L);
            origin.setLatitude(47.4979);
            origin.setLongitude(19.0402);

            City near = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            near.setId(2L);
            near.setLatitude(48.2082);
            near.setLongitude(16.3738);

            City far = buildCity("Paris", 0.8f, 0.7f, 0.5f, 0.6f, 0.3f, 0.55f, 0.4f, 0.5f, 0.2f);
            far.setId(3L);
            far.setLatitude(48.8566);
            far.setLongitude(2.3522);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findById(1L)).thenReturn(Optional.of(origin));
            when(cityRepository.findNearbyCities(eq(1L), anyDouble(), anyDouble(), eq(2000.0)))
                    .thenReturn(List.of(near, far));

            List<RecommendationResponse> results = recommendationService.getNearbyRecommendations(1L, 1L, 2000.0, 10);

            assertEquals(2, results.size());
            assertTrue(results.get(0).getSimilarityScore() >= results.get(1).getSimilarityScore());
        }
    }

    @Nested
    @DisplayName("getNearbyByCoordinates()")
    class GetNearbyByCoordinates {

        @Test
        @DisplayName("should use only proximity when userId is null")
        void shouldUseOnlyProximityForAnonymous() {
            City c = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            c.setId(2L);
            c.setLatitude(48.2082);
            c.setLongitude(16.3738);

            when(cityRepository.findNearbyCitiesByCoordinates(48.2082, 16.3738, 500.0))
                    .thenReturn(List.of(c));

            List<RecommendationResponse> results = recommendationService.getNearbyByCoordinates(
                    null, 48.2082, 16.3738, 500.0, 10);

            assertEquals(1, results.size());
            // verify preferencesRepository was never called (no user)
            verify(preferencesRepository, never()).findByUserId(anyLong());
        }

        @Test
        @DisplayName("should combine similarity + proximity when userId provided")
        void shouldCombineForLoggedInUser() {
            City c = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            c.setId(2L);
            c.setLatitude(48.2082);
            c.setLongitude(16.3738);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));
            when(cityRepository.findNearbyCitiesByCoordinates(48.2082, 16.3738, 500.0))
                    .thenReturn(List.of(c));

            List<RecommendationResponse> results = recommendationService.getNearbyByCoordinates(
                    1L, 48.2082, 16.3738, 500.0, 10);

            assertEquals(1, results.size());
            assertNotNull(results.get(0).getSimilarityScore());
            assertNotNull(results.get(0).getDistanceKm());
        }

        @Test
        @DisplayName("should fall back to proximity when user has no preferences")
        void shouldFallBackForUserWithoutPrefs() {
            City c = buildCity("Vienna", 0.85f, 0.75f, 0.4f, 0.65f, 0.3f, 0.55f, 0.4f, 0.5f, 0.15f);
            c.setId(2L);
            c.setLatitude(48.2082);
            c.setLongitude(16.3738);

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(cityRepository.findNearbyCitiesByCoordinates(48.2082, 16.3738, 500.0))
                    .thenReturn(List.of(c));

            List<RecommendationResponse> results = recommendationService.getNearbyByCoordinates(
                    1L, 48.2082, 16.3738, 500.0, 10);

            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("should return empty list when no nearby cities")
        void shouldReturnEmpty() {
            when(cityRepository.findNearbyCitiesByCoordinates(0.0, 0.0, 100.0))
                    .thenReturn(List.of());

            List<RecommendationResponse> results = recommendationService.getNearbyByCoordinates(
                    null, 0.0, 0.0, 100.0, 10);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("updatePreferences()")
    class UpdatePreferences {

        @Test
        @DisplayName("should not update for quick rating")
        void shouldNotUpdateForQuickRating() {
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).build();

            float oldCulture = prefs.getCultureWeight();
            float oldFood = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertEquals(oldCulture, prefs.getCultureWeight());
            assertEquals(oldFood, prefs.getFoodWeight());
        }

        @Test
        @DisplayName("should not update when preferences not found")
        void shouldNotUpdateWhenPrefsNotFound() {
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).cultureRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());

            // should not throw, just return early
            assertDoesNotThrow(() -> recommendationService.updatePreferences(user, rating));
        }

        @Test
        @DisplayName("should increase weight for high rating (5)")
        void shouldIncreaseForHighRating() {
            prefs.setFoodWeight(0.5f);
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).foodRating(5).build();

            float oldWeight = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertTrue(prefs.getFoodWeight() > oldWeight);
        }

        @Test
        @DisplayName("should decrease weight for low rating (1)")
        void shouldDecreaseForLowRating() {
            prefs.setFoodWeight(0.8f);
            Rating rating = Rating.builder()
                    .user(user).overallScore(2).foodRating(1).build();

            float oldWeight = prefs.getFoodWeight();
            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertTrue(prefs.getFoodWeight() < oldWeight);
        }

        @Test
        @DisplayName("should only update categories with non-null ratings")
        void shouldOnlyUpdateNonNullCategories() {
            float oldFood = prefs.getFoodWeight();
            float oldNightlife = prefs.getNightlifeWeight();
            float oldNature = prefs.getNatureWeight();

            Rating rating = Rating.builder()
                    .user(user).overallScore(4).cultureRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertEquals(oldFood, prefs.getFoodWeight());
            assertEquals(oldNightlife, prefs.getNightlifeWeight());
            assertEquals(oldNature, prefs.getNatureWeight());
        }

        @Test
        @DisplayName("should clamp weights to [0, 1] range")
        void shouldClampWeights() {
            prefs.setCultureWeight(0.99f);
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).cultureRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertTrue(prefs.getCultureWeight() <= 1.0f);
            assertTrue(prefs.getCultureWeight() >= 0.0f);
        }

        @Test
        @DisplayName("should increment per-category rating count after update")
        void shouldIncrementCategoryCount() {
            prefs.setFoodRatingCount(3);
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).foodRating(4).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertEquals(4, prefs.getFoodRatingCount());
        }

        @Test
        @DisplayName("should converge slower with more ratings (adaptive alpha)")
        void shouldHaveAdaptiveLearningRate() {
            // user with no prior food ratings — alpha is large (1/(0+1)=1.0)
            prefs.setFoodWeight(0.5f);
            prefs.setFoodRatingCount(0);
            Rating freshRating = Rating.builder()
                    .user(user).overallScore(5).foodRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, freshRating);
            float deltaFresh = Math.abs(prefs.getFoodWeight() - 0.5f);

            // user with many prior food ratings — alpha falls to baseAlpha (0.1)
            prefs.setFoodWeight(0.5f);
            prefs.setFoodRatingCount(100);
            Rating laterRating = Rating.builder()
                    .user(user).overallScore(5).foodRating(5).build();

            recommendationService.updatePreferences(user, laterRating);
            float deltaLater = Math.abs(prefs.getFoodWeight() - 0.5f);

            assertTrue(deltaFresh > deltaLater,
                    "First rating should move weight more than the 100th rating");
        }

        @Test
        @DisplayName("should update budget weight from costRating")
        void shouldUpdateBudgetFromCostRating() {
            prefs.setBudgetWeight(0.5f);
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).costRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertNotEquals(0.5f, prefs.getBudgetWeight());
            assertEquals(1, prefs.getBudgetRatingCount());
        }

        @Test
        @DisplayName("should update all 9 categories when all ratings provided")
        void shouldUpdateAllCategories() {
            Rating rating = Rating.builder()
                    .user(user).overallScore(5)
                    .cultureRating(5).foodRating(5).nightlifeRating(5)
                    .natureRating(5).safetyRating(5).costRating(5)
                    .beachRating(5).architectureRating(5).shoppingRating(5)
                    .build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            assertEquals(1, prefs.getCultureRatingCount());
            assertEquals(1, prefs.getFoodRatingCount());
            assertEquals(1, prefs.getNightlifeRatingCount());
            assertEquals(1, prefs.getNatureRatingCount());
            assertEquals(1, prefs.getSafetyRatingCount());
            assertEquals(1, prefs.getBudgetRatingCount());
            assertEquals(1, prefs.getBeachRatingCount());
            assertEquals(1, prefs.getArchitectureRatingCount());
            assertEquals(1, prefs.getShoppingRatingCount());
        }

        @Test
        @DisplayName("should not call save (relies on dirty checking)")
        void shouldNotCallSave() {
            Rating rating = Rating.builder()
                    .user(user).overallScore(5).cultureRating(5).build();

            when(preferencesRepository.findByUserId(1L)).thenReturn(Optional.of(prefs));

            recommendationService.updatePreferences(user, rating);

            verify(preferencesRepository, never()).save(any());
        }
    }
}