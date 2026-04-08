package com.travelRec.controller;

import com.travelRec.dto.rating.DetailedRatingRequest;
import com.travelRec.dto.rating.QuickRatingRequest;
import com.travelRec.dto.rating.RatingResponse;
import com.travelRec.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByTrip(@PathVariable Long tripId) {
        return ResponseEntity.ok(ratingService.getRatingsByTrip(tripId));
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByCity(@PathVariable Long cityId) {
        return ResponseEntity.ok(ratingService.getRatingsByCity(cityId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RatingResponse>> getRatingsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(ratingService.getRatingsByUser(userId));
    }

    @PostMapping("/quick")
    public ResponseEntity<RatingResponse> createQuickRating(@RequestParam Long userId,
                                                             @Valid @RequestBody QuickRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.createQuickRating(userId, request));
    }

    @PostMapping("/detailed")
    public ResponseEntity<RatingResponse> createDetailedRating(@RequestParam Long userId,
                                                                @Valid @RequestBody DetailedRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ratingService.createDetailedRating(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RatingResponse> updateRating(@PathVariable Long id,
                                                        @Valid @RequestBody DetailedRatingRequest request) {
        return ResponseEntity.ok(ratingService.updateRating(id, request));
    }
}
