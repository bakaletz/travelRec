package com.travelRec.controller;

import com.travelRec.dto.trip.AddCityToTripRequest;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @GetMapping
    public ResponseEntity<List<TripResponse>> getUserTrips(
            @RequestParam Long userId,
            @RequestParam(required = false) TripStatus status) {
        if (status != null) {
            return ResponseEntity.ok(tripService.getUserTripsByStatus(userId, status));
        }
        return ResponseEntity.ok(tripService.getUserTrips(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.getTripById(id));
    }

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@RequestParam Long userId,
                                                    @Valid @RequestBody TripRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripResponse> updateTrip(@PathVariable Long id,
                                                    @Valid @RequestBody TripRequest request) {
        return ResponseEntity.ok(tripService.updateTrip(id, request));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TripResponse> completeTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.completeTrip(id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable Long id) {
        return ResponseEntity.ok(tripService.cancelTrip(id));
    }

    @PostMapping("/{id}/cities")
    public ResponseEntity<TripResponse> addCityToTrip(@PathVariable Long id,
                                                       @Valid @RequestBody AddCityToTripRequest request) {
        return ResponseEntity.ok(tripService.addCityToTrip(id, request));
    }

    @DeleteMapping("/{id}/cities/{cityId}")
    public ResponseEntity<TripResponse> removeCityFromTrip(@PathVariable Long id,
                                                            @PathVariable Long cityId) {
        return ResponseEntity.ok(tripService.removeCityFromTrip(id, cityId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
        return ResponseEntity.noContent().build();
    }
}
