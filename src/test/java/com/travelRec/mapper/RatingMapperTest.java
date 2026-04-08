package com.travelRec.mapper;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RatingMapperTest {

    private RatingMapper mapper;
    private User user;
    private Trip trip;
    private City city;

    @BeforeEach
    void setUp() {
        mapper = new RatingMapper();

        user = User.builder()
                .id(1L)
                .email("anna@mail.com")
                .firstName("Anna")
                .lastName("Shevchenko")
                .role(Role.USER)
                .build();

        trip = Trip.builder()
                .id(1L)
                .name("Summer Trip")
                .status(TripStatus.COMPLETED)
                .build();

        Country country = Country.builder()
                .id(1L)
                .name("Turkey")
                .code("TR")
                .build();

        city = City.builder()
                .id(1L)
                .name("Istanbul")
                .country(country)
                .build();
    }

    @Nested
    @DisplayName("toEntity() from QuickRatingRequest")
    class QuickToEntity {

        @Test
        @DisplayName("should map quick rating fields")
        void shouldMapQuickRating() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(5)
                    .feedback("Loved it!")
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertEquals(5, rating.getOverallScore());
            assertEquals("Loved it!", rating.getFeedback());
            assertEquals(user, rating.getUser());
            assertEquals(trip, rating.getTrip());
            assertEquals(city, rating.getCity());
        }

        @Test
        @DisplayName("should leave detailed ratings null")
        void shouldLeaveDetailedNull() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(3)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertNull(rating.getCultureRating());
            assertNull(rating.getFoodRating());
            assertNull(rating.getNightlifeRating());
            assertNull(rating.getNatureRating());
            assertNull(rating.getSafetyRating());
            assertNull(rating.getCostRating());
            assertNull(rating.getBeachRating());
            assertNull(rating.getArchitectureRating());
            assertNull(rating.getShoppingRating());
        }

        @Test
        @DisplayName("should not be detailed")
        void shouldNotBeDetailed() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(1)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertFalse(rating.isDetailed());
        }

        @Test
        @DisplayName("should handle null feedback")
        void shouldHandleNullFeedback() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(5)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertNull(rating.getFeedback());
        }
    }

    @Nested
    @DisplayName("toEntity() from DetailedRatingRequest")
    class DetailedToEntity {

        @Test
        @DisplayName("should map all detailed fields")
        void shouldMapAllFields() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(4)
                    .cultureRating(5)
                    .foodRating(3)
                    .nightlifeRating(4)
                    .natureRating(2)
                    .safetyRating(3)
                    .costRating(4)
                    .beachRating(1)
                    .architectureRating(5)
                    .shoppingRating(3)
                    .feedback("Great city for culture")
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertEquals(4, rating.getOverallScore());
            assertEquals(5, rating.getCultureRating());
            assertEquals(3, rating.getFoodRating());
            assertEquals(4, rating.getNightlifeRating());
            assertEquals(2, rating.getNatureRating());
            assertEquals(3, rating.getSafetyRating());
            assertEquals(4, rating.getCostRating());
            assertEquals(1, rating.getBeachRating());
            assertEquals(5, rating.getArchitectureRating());
            assertEquals(3, rating.getShoppingRating());
            assertEquals("Great city for culture", rating.getFeedback());
        }

        @Test
        @DisplayName("should be detailed")
        void shouldBeDetailed() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(4)
                    .cultureRating(5)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should set entity references")
        void shouldSetReferences() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(4)
                    .cultureRating(5)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertEquals(user, rating.getUser());
            assertEquals(trip, rating.getTrip());
            assertEquals(city, rating.getCity());
        }

        @Test
        @DisplayName("should handle partial detailed ratings")
        void shouldHandlePartialRatings() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L)
                    .cityId(1L)
                    .overallScore(4)
                    .foodRating(5)
                    .natureRating(3)
                    .build();

            Rating rating = mapper.toEntity(request, user, trip, city);

            assertEquals(5, rating.getFoodRating());
            assertEquals(3, rating.getNatureRating());
            assertNull(rating.getCultureRating());
            assertNull(rating.getNightlifeRating());
            assertNull(rating.getSafetyRating());
            assertNull(rating.getCostRating());
            assertNull(rating.getBeachRating());
            assertNull(rating.getArchitectureRating());
            assertNull(rating.getShoppingRating());
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all fields for detailed rating")
        void shouldMapDetailedRating() {
            Rating rating = Rating.builder()
                    .id(1L)
                    .user(user)
                    .trip(trip)
                    .city(city)
                    .overallScore(4)
                    .cultureRating(5)
                    .foodRating(3)
                    .nightlifeRating(4)
                    .natureRating(2)
                    .safetyRating(3)
                    .costRating(4)
                    .beachRating(1)
                    .architectureRating(5)
                    .shoppingRating(3)
                    .feedback("Great experience")
                    .createdAt(LocalDateTime.of(2026, 7, 25, 10, 0))
                    .build();

            RatingResponse response = mapper.toResponse(rating);

            assertEquals(1L, response.getId());
            assertEquals(1L, response.getTripId());
            assertEquals(1L, response.getCityId());
            assertEquals("Istanbul", response.getCityName());
            assertEquals(4, response.getOverallScore());
            assertEquals(5, response.getCultureRating());
            assertEquals(3, response.getFoodRating());
            assertEquals(4, response.getNightlifeRating());
            assertEquals(2, response.getNatureRating());
            assertEquals(3, response.getSafetyRating());
            assertEquals(4, response.getCostRating());
            assertEquals(1, response.getBeachRating());
            assertEquals(5, response.getArchitectureRating());
            assertEquals(3, response.getShoppingRating());
            assertEquals("Great experience", response.getFeedback());
            assertTrue(response.isDetailed());
            assertEquals(LocalDateTime.of(2026, 7, 25, 10, 0), response.getCreatedAt());
        }

        @Test
        @DisplayName("should map quick rating as not detailed")
        void shouldMapQuickRating() {
            Rating rating = Rating.builder()
                    .id(2L)
                    .user(user)
                    .trip(trip)
                    .city(city)
                    .overallScore(5)
                    .build();

            RatingResponse response = mapper.toResponse(rating);

            assertEquals(5, response.getOverallScore());
            assertFalse(response.isDetailed());
            assertNull(response.getCultureRating());
            assertNull(response.getFoodRating());
        }

        @Test
        @DisplayName("should map city name from entity")
        void shouldMapCityName() {
            Rating rating = Rating.builder()
                    .id(1L)
                    .user(user)
                    .trip(trip)
                    .city(city)
                    .overallScore(4)
                    .build();

            RatingResponse response = mapper.toResponse(rating);

            assertEquals("Istanbul", response.getCityName());
        }
    }
}
