package com.travelRec.service;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.RatingMapper;
import com.travelRec.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    private Trip trip;
    private City city;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("anna@mail.com").firstName("Anna").lastName("S").role(Role.USER).build();

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
    @DisplayName("createQuickRating()")
    class CreateQuickRating {

        @Test
        @DisplayName("should create quick rating for completed trip")
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
        @DisplayName("should not trigger preference update for quick-only fields")
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
    }

    @Nested
    @DisplayName("createRating() - trip status transitions")
    class TripStatusTransitions {

        @Test
        @DisplayName("should mark trip as RATED after first rating")
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
        @DisplayName("should allow rating already RATED trip")
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
        }
    }
}
