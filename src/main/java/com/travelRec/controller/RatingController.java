package com.travelRec.controller;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.security.CustomUserDetails;
import com.travelRec.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByTrip(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long tripId) {
        return ResponseEntity.ok(ratingService.getRatingsByTrip(user.getId(), tripId));
    }

    @GetMapping("/user/me")
    public ResponseEntity<List<RatingResponse>> getCurrentUserRatings(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ratingService.getRatingsByUser(user.getId()));
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByCity(@PathVariable Long cityId) {
        return ResponseEntity.ok(ratingService.getRatingsByCity(cityId));
    }

    @PostMapping("/quick")
    public ResponseEntity<RatingResponse> createQuickRating(@AuthenticationPrincipal CustomUserDetails user,
                                                            @Valid @RequestBody QuickRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.createQuickRating(user.getId(), request));
    }

    @PostMapping("/detailed")
    public ResponseEntity<RatingResponse> createDetailedRating(@AuthenticationPrincipal CustomUserDetails user,
                                                               @Valid @RequestBody DetailedRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.createDetailedRating(user.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RatingResponse> updateRating(@AuthenticationPrincipal CustomUserDetails user,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody DetailedRatingRequest request) {
        return ResponseEntity.ok(ratingService.updateRating(user.getId(), id, request));
    }
}