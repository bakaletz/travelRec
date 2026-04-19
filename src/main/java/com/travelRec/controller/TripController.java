package com.travelRec.controller;

import com.travelRec.dto.trip.AddCityToTripRequest;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.security.CustomUserDetails;
import com.travelRec.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<List<TripResponse>> getUserTrips(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) TripStatus status) {
        if (status != null) {
            return ResponseEntity.ok(tripService.getUserTripsByStatus(user.getId(), status));
        }
        return ResponseEntity.ok(tripService.getUserTrips(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(user.getId(), id));
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                   @Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(user.getId(), request));
    }

    @PostMapping("/{id}/cities")
    public ResponseEntity<TripResponse> addCityToTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody AddCityToTripRequest request) {
        return ResponseEntity.ok(tripService.addCityToTrip(user.getId(), id, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(user.getId(), id, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TripResponse> completeTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                     @PathVariable Long id) {
        return ResponseEntity.ok(tripService.completeTrip(user.getId(), id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(tripService.cancelTrip(user.getId(), id));
    }

    @PatchMapping("/{id}/reorder")
    public ResponseEntity<TripResponse> reorderCities(@AuthenticationPrincipal CustomUserDetails user,
                                                      @PathVariable Long id,
                                                      @RequestBody List<Long> cityIds) {
        return ResponseEntity.ok(tripService.reorderCities(user.getId(), id, cityIds));
    }

    @PatchMapping("/{id}/optimize")
    public ResponseEntity<TripResponse> optimizeRoute(@AuthenticationPrincipal CustomUserDetails user,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(tripService.optimizeRoute(user.getId(), id));
    }

    @DeleteMapping("/{id}/cities/{cityId}")
    public ResponseEntity<TripResponse> removeCityFromTrip(@AuthenticationPrincipal CustomUserDetails user,
                                                           @PathVariable Long id,
                                                           @PathVariable Long cityId) {
        return ResponseEntity.ok(tripService.removeCityFromTrip(user.getId(), id, cityId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@AuthenticationPrincipal CustomUserDetails user,
                                           @PathVariable Long id) {
        tripService.deleteTrip(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}