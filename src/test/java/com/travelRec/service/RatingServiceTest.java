package com.travelRec.service;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.RatingMapper;
import com.travelRec.repository.RatingRepository;
import com.travelRec.service.recommendation.RecommendationService;
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
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private TripService tripService;

    @Mock
    private UserService userService;

    @Mock
    private CityService cityService;

    @Mock
    private RecommendationService recommendationService;

    @Spy
    private RatingMapper ratingMapper = new RatingMapper();

    @InjectMocks
    private RatingService ratingService;

    private User user;
    private User otherUser;
    private Trip trip;
    private City city;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("anna@mail.com").firstName("Anna").lastName("S").role(Role.USER).build();
        otherUser = User.builder().id(2L).email("bob@mail.com").firstName("Bob").lastName("X").role(Role.USER).build();

        trip = Trip.builder()
                .id(1L).user(user).name("Summer Trip")
                .status(TripStatus.COMPLETED)
                .build();

        Country country = Country.builder().id(1L).name("Turkey").code("TR").build();
        city = City.builder()
                .id(1L).country(country).name("Istanbul")
                .cityType(CityType.MEGAPOLIS).climateType(ClimateType.TEMPERATE)
                .cultureScore(0.85f).foodScore(0.9f).nightlifeScore(0.7f)
                .natureScore(0.5f).safetyScore(0.6f).costLevel(0.4f)
                .beachScore(0.3f).architectureScore(0.9f).shoppingScore(0.8f)
                .build();
    }

    @Nested
    @DisplayName("getRatingsByTrip()")
    class GetRatingsByTrip {

        @Test
        @DisplayName("should return ratings for owned trip")
        void shouldReturn() {
            Rating rating = Rating.builder()
                    .id(1L).user(user).trip(trip).city(city).overallScore(5).build();

            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(ratingRepository.findByTripId(1L)).thenReturn(List.of(rating));

            List<RatingResponse> result = ratingService.getRatingsByTrip(1L, 1L);

            assertEquals(1, result.size());
            assertEquals(5, result.get(0).getOverallScore());
        }

        @Test
        @DisplayName("should throw AccessDenied when trip belongs to another user")
        void shouldThrowAccessDenied() {
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);

            assertThrows(AccessDeniedException.class, () -> ratingService.getRatingsByTrip(2L, 1L));
        }

        @Test
        @DisplayName("should return empty list when no ratings")
        void shouldReturnEmpty() {
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(ratingRepository.findByTripId(1L)).thenReturn(List.of());

            assertTrue(ratingService.getRatingsByTrip(1L, 1L).isEmpty());
        }
    }

    @Nested
    @DisplayName("getRatingsByCity()")
    class GetRatingsByCity {

        @Test
        @DisplayName("should return all ratings for a city")
        void shouldReturn() {
            Rating rating = Rating.builder()
                    .id(1L).user(user).trip(trip).city(city).overallScore(4).build();

            when(ratingRepository.findByCityId(1L)).thenReturn(List.of(rating));

            List<RatingResponse> result = ratingService.getRatingsByCity(1L);

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("should not require ownership check (public)")
        void shouldNotRequireOwnership() {
            when(ratingRepository.findByCityId(1L)).thenReturn(List.of());

            assertDoesNotThrow(() -> ratingService.getRatingsByCity(1L));
            verify(tripService, never()).findTripOrThrow(any());
        }
    }

    @Nested
    @DisplayName("getRatingsByUser()")
    class GetRatingsByUser {

        @Test
        @DisplayName("should return all ratings by user")
        void shouldReturn() {
            Rating rating = Rating.builder()
                    .id(1L).user(user).trip(trip).city(city).overallScore(4).build();

            when(ratingRepository.findByUserId(1L)).thenReturn(List.of(rating));

            List<RatingResponse> result = ratingService.getRatingsByUser(1L);

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("createQuickRating()")
    class CreateQuickRating {

        @Test
        @DisplayName("should create quick rating for COMPLETED trip")
        void shouldCreateQuickRating() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5).feedback("Loved it!").build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            RatingResponse response = ratingService.createQuickRating(1L, request);

            assertEquals(5, response.getOverallScore());
            assertFalse(response.isDetailed());
            verify(cityService).recalculateScores(1L);
        }

        @Test
        @DisplayName("should throw AccessDenied when rating someone else's trip")
        void shouldThrowAccessDenied() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5).build();

            when(userService.findUserOrThrow(2L)).thenReturn(otherUser);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);

            assertThrows(AccessDeniedException.class, () -> ratingService.createQuickRating(2L, request));
        }

        @Test
        @DisplayName("should throw when trip is PLANNED")
        void shouldThrowForPlannedTrip() {
            trip.setStatus(TripStatus.PLANNED);
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(3).build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);

            assertThrows(IllegalStateException.class, () -> ratingService.createQuickRating(1L, request));
        }

        @Test
        @DisplayName("should throw when trip is CANCELLED")
        void shouldThrowForCancelledTrip() {
            trip.setStatus(TripStatus.CANCELLED);
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(3).build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);

            assertThrows(IllegalStateException.class, () -> ratingService.createQuickRating(1L, request));
        }

        @Test
        @DisplayName("should throw when already rated")
        void shouldThrowWhenDuplicate() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4).build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> ratingService.createQuickRating(1L, request));
        }
    }

    @Nested
    @DisplayName("createDetailedRating()")
    class CreateDetailedRating {

        @Test
        @DisplayName("should create detailed rating and trigger preference update")
        void shouldCreateAndUpdatePreferences() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4)
                    .cultureRating(5).foodRating(3).nightlifeRating(4)
                    .build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            RatingResponse response = ratingService.createDetailedRating(1L, request);

            assertTrue(response.isDetailed());
            assertEquals(5, response.getCultureRating());
            verify(recommendationService).updatePreferences(eq(user), any(Rating.class));
            verify(cityService).recalculateScores(1L);
        }

        @Test
        @DisplayName("should not trigger preference update when no detailed fields")
        void shouldNotUpdateForQuickOnly() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5)
                    .build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            ratingService.createDetailedRating(1L, request);

            verify(recommendationService, never()).updatePreferences(any(), any());
        }

        @Test
        @DisplayName("should throw AccessDenied when rating someone else's trip")
        void shouldThrowAccessDenied() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4).cultureRating(5).build();

            when(userService.findUserOrThrow(2L)).thenReturn(otherUser);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);

            assertThrows(AccessDeniedException.class, () -> ratingService.createDetailedRating(2L, request));
        }
    }

    @Nested
    @DisplayName("trip status transitions")
    class TripStatusTransitions {

        @Test
        @DisplayName("should mark COMPLETED trip as RATED after first rating")
        void shouldMarkAsRated() {
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5).build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            ratingService.createQuickRating(1L, request);

            assertEquals(TripStatus.RATED, trip.getStatus());
        }

        @Test
        @DisplayName("should keep RATED status when rating already-rated trip")
        void shouldAllowRatingRatedTrip() {
            trip.setStatus(TripStatus.RATED);
            QuickRatingRequest request = QuickRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(4).build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripService.findTripOrThrow(1L)).thenReturn(trip);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(ratingRepository.existsByUserIdAndTripIdAndCityId(1L, 1L, 1L)).thenReturn(false);
            when(ratingRepository.save(any(Rating.class))).thenAnswer(inv -> {
                Rating r = inv.getArgument(0);
                r.setId(1L);
                return r;
            });

            assertDoesNotThrow(() -> ratingService.createQuickRating(1L, request));
            // status stays RATED — markAsRated() is only called from COMPLETED
            assertEquals(TripStatus.RATED, trip.getStatus());
        }
    }

    @Nested
    @DisplayName("updateRating()")
    class UpdateRating {

        @Test
        @DisplayName("should update all fields of own rating via dirty checking")
        void shouldUpdate() {
            Rating existing = Rating.builder()
                    .id(10L).user(user).trip(trip).city(city)
                    .overallScore(3).cultureRating(3).build();

            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5)
                    .cultureRating(5).foodRating(4).nightlifeRating(3)
                    .natureRating(4).safetyRating(5).costRating(3)
                    .beachRating(2).architectureRating(5).shoppingRating(4)
                    .feedback("Updated feedback")
                    .build();

            when(ratingRepository.findById(10L)).thenReturn(Optional.of(existing));

            RatingResponse response = ratingService.updateRating(1L, 10L, request);

            assertEquals(5, response.getOverallScore());
            assertEquals(5, response.getCultureRating());
            assertEquals(4, response.getFoodRating());
            assertEquals("Updated feedback", response.getFeedback());

            verify(cityService).recalculateScores(1L);
            verify(recommendationService).updatePreferences(eq(user), any(Rating.class));
            verify(ratingRepository, never()).save(any(Rating.class));
        }

        @Test
        @DisplayName("should throw when rating not found")
        void shouldThrowWhenNotFound() {
            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5).build();

            when(ratingRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> ratingService.updateRating(1L, 99L, request));
        }

        @Test
        @DisplayName("should throw AccessDenied when updating someone else's rating")
        void shouldThrowAccessDenied() {
            Rating existing = Rating.builder()
                    .id(10L).user(user).trip(trip).city(city)
                    .overallScore(3).build();

            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5).build();

            when(ratingRepository.findById(10L)).thenReturn(Optional.of(existing));

            assertThrows(AccessDeniedException.class,
                    () -> ratingService.updateRating(2L, 10L, request));
        }

        @Test
        @DisplayName("should not call updatePreferences when updated rating has no detailed fields")
        void shouldNotUpdatePrefsWhenNotDetailed() {
            Rating existing = Rating.builder()
                    .id(10L).user(user).trip(trip).city(city)
                    .overallScore(3).cultureRating(3).build();

            DetailedRatingRequest request = DetailedRatingRequest.builder()
                    .tripId(1L).cityId(1L).overallScore(5)
                    .build();

            when(ratingRepository.findById(10L)).thenReturn(Optional.of(existing));

            ratingService.updateRating(1L, 10L, request);

            verify(recommendationService, never()).updatePreferences(any(), any());
            verify(cityService).recalculateScores(1L);
        }
    }
}