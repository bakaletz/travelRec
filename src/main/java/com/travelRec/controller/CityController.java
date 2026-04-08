package com.travelRec.controller;

import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<List<CityResponse>> getCities(
            @RequestParam(required = false) Continent continent,
            @RequestParam(required = false) CityType cityType,
            @RequestParam(required = false) ClimateType climateType,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) String search) {
        if (search != null) return ResponseEntity.ok(cityService.searchCities(search));
        if (continent != null) return ResponseEntity.ok(cityService.getCitiesByContinent(continent));
        if (cityType != null) return ResponseEntity.ok(cityService.getCitiesByCityType(cityType));
        if (climateType != null) return ResponseEntity.ok(cityService.getCitiesByClimateType(climateType));
        if (countryId != null) return ResponseEntity.ok(cityService.getCitiesByCountry(countryId));
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    @GetMapping("/{id}/nearby")
    public ResponseEntity<List<CityResponse>> getNearbyCities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "300") double radiusKm) {
        return ResponseEntity.ok(cityService.getNearbyCities(id, radiusKm));
    }

    @GetMapping("/{id}/same-country")
    public ResponseEntity<List<CityResponse>> getCitiesInSameCountry(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCitiesInSameCountry(id));
    }

    @PostMapping
    public ResponseEntity<CityResponse> createCity(@Valid @RequestBody CityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.createCity(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityResponse> updateCity(@PathVariable Long id,
                                                    @Valid @RequestBody CityRequest request) {
        return ResponseEntity.ok(cityService.updateCity(id, request));
    }

    @PatchMapping("/{id}/recalculate")
    public ResponseEntity<Void> recalculateScores(@PathVariable Long id) {
        cityService.recalculateScores(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCity(@PathVariable Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
