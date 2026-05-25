package com.travelRec.controller;

import com.travelRec.dto.city.CityResponse;
import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.service.recommendation.RecommendationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.travelRec.repository.UserRepository;
import com.travelRec.security.JwtUtil;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private RequestPostProcessor authUser;

    @BeforeEach
    void setUp() {
        authUser = TestAuth.user();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RecommendationResponse buildResponse() {
        return RecommendationResponse.builder()
                .city(CityResponse.builder().id(1L).name("Budapest").build())
                .similarityScore(0.85)
                .build();
    }

    @Nested
    @DisplayName("GET /api/recommendations/personalized")
    class Personalized {

        @Test
        @DisplayName("should use default limit when not provided")
        void shouldUseDefaultLimit() throws Exception {
            when(recommendationService.getPersonalized(eq(1L), eq(10), any(), any(), any()))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/recommendations/personalized").with(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].city.name").value("Budapest"));

            verify(recommendationService).getPersonalized(eq(1L), eq(10), any(), any(), any());
        }

        @Test
        @DisplayName("should pass custom limit and filters")
        void shouldPassFilters() throws Exception {
            when(recommendationService.getPersonalized(
                    eq(1L), eq(20),
                    eq(List.of(Continent.EUROPE)),
                    eq(List.of(CityType.LARGE_CITY)),
                    eq(List.of(ClimateType.MEDITERRANEAN))))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/personalized")
                            .with(authUser)
                            .param("limit", "20")
                            .param("continent", "EUROPE")
                            .param("cityType", "LARGE_CITY")
                            .param("climateType", "MEDITERRANEAN"))
                    .andExpect(status().isOk());

            verify(recommendationService).getPersonalized(
                    eq(1L), eq(20),
                    eq(List.of(Continent.EUROPE)),
                    eq(List.of(CityType.LARGE_CITY)),
                    eq(List.of(ClimateType.MEDITERRANEAN)));
        }

        @Test
        @DisplayName("should accept multi-valued filter params")
        void shouldAcceptMultipleFilters() throws Exception {
            when(recommendationService.getPersonalized(
                    eq(1L), eq(10),
                    eq(List.of(Continent.EUROPE, Continent.ASIA)),
                    any(), any()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/personalized")
                            .with(authUser)
                            .param("continent", "EUROPE", "ASIA"))
                    .andExpect(status().isOk());

            verify(recommendationService).getPersonalized(
                    eq(1L), eq(10),
                    eq(List.of(Continent.EUROPE, Continent.ASIA)),
                    any(), any());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/popular")
    class Popular {

        @Test
        @DisplayName("should return popular cities (no auth required)")
        void shouldReturnPopular() throws Exception {
            when(recommendationService.getPopular(10)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/recommendations/popular"))
                    .andExpect(status().isOk());

            verify(recommendationService).getPopular(10);
        }

        @Test
        @DisplayName("should accept custom limit")
        void shouldAcceptLimit() throws Exception {
            when(recommendationService.getPopular(5)).thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/popular").param("limit", "5"))
                    .andExpect(status().isOk());

            verify(recommendationService).getPopular(5);
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/similar")
    class Similar {

        @Test
        @DisplayName("should return similar cities with default limit 6")
        void shouldReturnSimilar() throws Exception {
            when(recommendationService.getSimilarCities(1L, 6))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/recommendations/similar").param("cityId", "1"))
                    .andExpect(status().isOk());

            verify(recommendationService).getSimilarCities(1L, 6);
        }

        @Test
        @DisplayName("should pass custom limit")
        void shouldPassCustomLimit() throws Exception {
            when(recommendationService.getSimilarCities(1L, 12))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/similar")
                            .param("cityId", "1")
                            .param("limit", "12"))
                    .andExpect(status().isOk());

            verify(recommendationService).getSimilarCities(1L, 12);
        }

        @Test
        @DisplayName("should return 400 when cityId is missing")
        void shouldRejectMissingCityId() throws Exception {
            mockMvc.perform(get("/api/recommendations/similar"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/because-you-liked")
    class BecauseYouLiked {

        @Test
        @DisplayName("should return recommendations")
        void shouldReturn() throws Exception {
            when(recommendationService.getBecauseYouLiked(1L, 10))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/recommendations/because-you-liked").with(authUser))
                    .andExpect(status().isOk());

            verify(recommendationService).getBecauseYouLiked(1L, 10);
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/nearby")
    class Nearby {

        @Test
        @DisplayName("should use default radius 300 km")
        void shouldUseDefaultRadius() throws Exception {
            when(recommendationService.getNearbyRecommendations(1L, 1L, 300.0, 10))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/nearby")
                            .with(authUser)
                            .param("cityId", "1"))
                    .andExpect(status().isOk());

            verify(recommendationService).getNearbyRecommendations(1L, 1L, 300.0, 10);
        }

        @Test
        @DisplayName("should pass custom radius and limit")
        void shouldPassCustomParams() throws Exception {
            when(recommendationService.getNearbyRecommendations(1L, 1L, 500.0, 20))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/nearby")
                            .with(authUser)
                            .param("cityId", "1")
                            .param("radiusKm", "500")
                            .param("limit", "20"))
                    .andExpect(status().isOk());

            verify(recommendationService).getNearbyRecommendations(1L, 1L, 500.0, 20);
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/nearby-me")
    class NearbyMe {

        @Test
        @DisplayName("should pass userId for authenticated request")
        void shouldPassUserId() throws Exception {
            when(recommendationService.getNearbyByCoordinates(1L, 47.5, 19.0, 500.0, 10))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/recommendations/nearby-me")
                            .with(authUser)
                            .param("lat", "47.5")
                            .param("lng", "19.0"))
                    .andExpect(status().isOk());

            verify(recommendationService).getNearbyByCoordinates(1L, 47.5, 19.0, 500.0, 10);
        }

        @Test
        @DisplayName("should use defaults for radius and limit")
        void shouldUseDefaults() throws Exception {
            when(recommendationService.getNearbyByCoordinates(eq(1L), eq(0.0), eq(0.0), eq(500.0), eq(10)))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/recommendations/nearby-me")
                            .with(authUser)
                            .param("lat", "0")
                            .param("lng", "0"))
                    .andExpect(status().isOk());

            verify(recommendationService).getNearbyByCoordinates(1L, 0.0, 0.0, 500.0, 10);
        }

        @Test
        @DisplayName("should return 400 when lat/lng missing")
        void shouldRejectMissingCoords() throws Exception {
            mockMvc.perform(get("/api/recommendations/nearby-me").with(authUser))
                    .andExpect(status().isBadRequest());
        }
    }
}