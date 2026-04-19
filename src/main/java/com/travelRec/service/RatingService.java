package com.travelRec.service;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.mapper.RatingMapper;
import com.travelRec.repository.RatingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RatingService {

    private final RatingRepository ratingRepository;
    private final TripService tripService;
    private final UserService userService;
    private final CityService cityService;
    private final RecommendationService recommendationService;
    private final RatingMapper ratingMapper;

    public List<RatingResponse> getRatingsByTrip(Long userId, Long tripId) {
        Trip trip = tripService.findTripOrThrow(tripId);
        if (!trip.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have access to this trip's ratings");
        }
        return ratingRepository.findByTripId(tripId).stream()
                .map(ratingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RatingResponse> getRatingsByCity(Long cityId) {
        return ratingRepository.findByCityId(cityId).stream()
                .map(ratingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RatingResponse> getRatingsByUser(Long userId) {
        return ratingRepository.findByUserId(userId).stream()
                .map(ratingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RatingResponse createQuickRating(Long userId, QuickRatingRequest request) {
        User user = userService.findUserOrThrow(userId);
        Trip trip = tripService.findTripOrThrow(request.getTripId());
        City city = cityService.findCityOrThrow(request.getCityId());

        validateCanCreate(userId, trip, request.getTripId(), request.getCityId());

        Rating rating = ratingMapper.toEntity(request, user, trip, city);
        Rating saved = ratingRepository.save(rating);

        afterRating(saved, trip);

        return ratingMapper.toResponse(saved);
    }

    @Transactional
    public RatingResponse createDetailedRating(Long userId, DetailedRatingRequest request) {
        User user = userService.findUserOrThrow(userId);
        Trip trip = tripService.findTripOrThrow(request.getTripId());
        City city = cityService.findCityOrThrow(request.getCityId());

        validateCanCreate(userId, trip, request.getTripId(), request.getCityId());

        Rating rating = ratingMapper.toEntity(request, user, trip, city);
        Rating saved = ratingRepository.save(rating);

        afterRating(saved, trip);

        return ratingMapper.toResponse(saved);
    }

    @Transactional
    public RatingResponse updateRating(Long userId, Long ratingId, DetailedRatingRequest request) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new EntityNotFoundException("Rating not found with id: " + ratingId));

        if (!rating.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only update your own ratings");
        }

        rating.setOverallScore(request.getOverallScore());
        rating.setCultureRating(request.getCultureRating());
        rating.setFoodRating(request.getFoodRating());
        rating.setNightlifeRating(request.getNightlifeRating());
        rating.setNatureRating(request.getNatureRating());
        rating.setSafetyRating(request.getSafetyRating());
        rating.setCostRating(request.getCostRating());
        rating.setBeachRating(request.getBeachRating());
        rating.setArchitectureRating(request.getArchitectureRating());
        rating.setShoppingRating(request.getShoppingRating());
        rating.setFeedback(request.getFeedback());

        cityService.recalculateScores(rating.getCity().getId());
        if (rating.isDetailed()) {
            recommendationService.updatePreferences(rating.getUser(), rating);
        }

        return ratingMapper.toResponse(rating);
    }

    private void validateCanCreate(Long userId, Trip trip, Long tripId, Long cityId) {
        if (!trip.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only rate your own trips");
        }

        if (trip.getStatus() != TripStatus.COMPLETED && trip.getStatus() != TripStatus.RATED) {
            throw new IllegalStateException("Can only rate COMPLETED or RATED trips");
        }

        if (ratingRepository.existsByUserIdAndTripIdAndCityId(userId, tripId, cityId)) {
            throw new IllegalArgumentException("You have already rated this city in this trip");
        }
    }

    private void afterRating(Rating rating, Trip trip) {
        cityService.recalculateScores(rating.getCity().getId());

        if (rating.isDetailed()) {
            recommendationService.updatePreferences(rating.getUser(), rating);
        }

        if (trip.getStatus() == TripStatus.COMPLETED) {
            trip.markAsRated();
        }
    }
}