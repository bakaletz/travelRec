package com.travelRec.controller;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.security.CustomUserDetails;
import com.travelRec.service.RecommendationService;
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
            @RequestParam(required = false) Continent continent,
            @RequestParam(required = false) CityType cityType,
            @RequestParam(required = false) ClimateType climateType) {
        return ResponseEntity.ok(recommendationService.getPersonalized(user.getId(), limit, continent, cityType, climateType));
    }

    @GetMapping("/popular")
    public ResponseEntity<List<RecommendationResponse>> getPopular(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getPopular(limit));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RecommendationResponse>> getNearbyRecommendations(
            @RequestParam Long userId,
            @RequestParam Long cityId,
            @RequestParam(defaultValue = "300") double radiusKm,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(recommendationService.getNearbyRecommendations(userId, cityId, radiusKm, limit));
    }
}
