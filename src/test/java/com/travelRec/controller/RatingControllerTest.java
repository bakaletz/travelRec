package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.service.RatingService;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RatingController.class)
@AutoConfigureMockMvc(addFilters = false)
class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private RatingService ratingService;

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

    private RatingResponse buildResponse() {
        return RatingResponse.builder()
                .id(1L).tripId(1L).cityId(1L).cityName("Istanbul")
                .overallScore(5).build();
    }

    @Nested
    @DisplayName("GET /api/ratings/trip/{tripId}")
    class GetByTrip {

        @Test
        @DisplayName("should return ratings for owned trip")
        void shouldReturn() throws Exception {
            when(ratingService.getRatingsByTrip(1L, 1L)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/ratings/trip/1").with(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].overallScore").value(5));

            verify(ratingService).getRatingsByTrip(1L, 1L);
        }
    }

    @Nested
    @DisplayName("GET /api/ratings/user/me")
    class GetCurrentUserRatings {

        @Test
        @DisplayName("should return current user ratings")
        void shouldReturn() throws Exception {
            when(ratingService.getRatingsByUser(1L)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/ratings/user/me").with(authUser))
                    .andExpect(status().isOk());

            verify(ratingService).getRatingsByUser(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/ratings/city/{cityId}")
    class GetByCity {

        @Test
        @DisplayName("should return ratings for city (public)")
        void shouldReturn() throws Exception {
            when(ratingService.getRatingsByCity(1L)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/ratings/city/1"))
                    .andExpect(status().isOk());

            verify(ratingService).getRatingsByCity(1L);
        }
    }

    @Nested
    @DisplayName("POST /api/ratings/quick")
    class QuickRating {

        @Test
        @DisplayName("should create quick rating and return 201")
        void shouldCreate() throws Exception {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5)
                    .feedback("Loved it!").build();

            when(ratingService.createQuickRating(eq(1L), any(QuickRatingRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(post("/api/ratings/quick")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(ratingService).createQuickRating(eq(1L), any(QuickRatingRequest.class));
        }

        @Test
        @DisplayName("should return 400 when overall score out of range")
        void shouldRejectInvalidScore() throws Exception {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(10).build();

            mockMvc.perform(post("/api/ratings/quick")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when tripId is null")
        void shouldRejectNullTripId() throws Exception {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .cityId(1L).overallScore(5).build();

            mockMvc.perform(post("/api/ratings/quick")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/ratings/detailed")
    class DetailedRating {

        @Test
        @DisplayName("should create detailed rating and return 201")
        void shouldCreate() throws Exception {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4)
                    .cultureRating(5).foodRating(3).build();

            when(ratingService.createDetailedRating(eq(1L), any(DetailedRatingRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(post("/api/ratings/detailed")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 when sub-rating out of range")
        void shouldRejectInvalidSubRating() throws Exception {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4)
                    .cultureRating(7).build();

            mockMvc.perform(post("/api/ratings/detailed")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/ratings/{id}")
    class UpdateRating {

        @Test
        @DisplayName("should update rating and return 200")
        void shouldUpdate() throws Exception {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5)
                    .cultureRating(5).build();

            when(ratingService.updateRating(eq(1L), eq(10L), any(DetailedRatingRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(put("/api/ratings/10")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(ratingService).updateRating(eq(1L), eq(10L), any(DetailedRatingRequest.class));
        }
    }
}