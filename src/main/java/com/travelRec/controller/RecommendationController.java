package com.travelRec.controller;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.dto.recommendation.TripRecommendationResponse;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.security.CustomUserDetails;
import com.travelRec.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/personalized")
    public ResponseEntity<List<RecommendationResponse>> getPersonalized(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) List<Continent> continent,
            @RequestParam(required = false) List<CityType> cityType,
            @RequestParam(required = false) List<ClimateType> climateType) {
        return ResponseEntity.ok(recommendationService.getPersonalized(user.getId(), limit, continent, cityType, climateType));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<RecommendationResponse>> getPopular(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getPopular(limit));
    }

    @GetMapping("/similar")
    public ResponseEntity<List<RecommendationResponse>> getSimilarCities(
            @RequestParam Long cityId,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(recommendationService.getSimilarCities(cityId, limit));
    }

    @GetMapping("/because-you-liked")
    public ResponseEntity<List<RecommendationResponse>> getBecauseYouLiked(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getBecauseYouLiked(user.getId(), limit));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RecommendationResponse>> getNearbyRecommendations(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long cityId,
            @RequestParam(defaultValue = "300") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getNearbyRecommendations(user.getId(), cityId, radiusKm, limit));
    }

    @GetMapping("/nearby-me")
    public ResponseEntity<List<RecommendationResponse>> getNearbyByCoordinates(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "500") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = user != null ? user.getId() : null;
        return ResponseEntity.ok(recommendationService.getNearbyByCoordinates(userId, lat, lng, radiusKm, limit));
    }

    @GetMapping("/trips")
    public ResponseEntity<List<TripRecommendationResponse>> getRecommendedTrips(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) List<Continent> continent) {
        return ResponseEntity.ok(recommendationService.getRecommendedTrips(user.getId(), continent));
    }

    @GetMapping("/by-country/{countryId}")
    public ResponseEntity<List<RecommendationResponse>> getCountryCityMatches(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long countryId) {
        return ResponseEntity.ok(recommendationService.getCountryCityMatches(user.getId(), countryId));
    }

    @GetMapping("/match/{cityId}")
    public ResponseEntity<RecommendationResponse> getMatch(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long cityId) {
        return ResponseEntity.ok(recommendationService.getMatch(user.getId(), cityId));
    }
}