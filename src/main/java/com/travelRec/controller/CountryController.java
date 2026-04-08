package com.travelRec.controller;

import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.enums.Continent;
import com.travelRec.service.CountryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    public ResponseEntity<List<CountryResponse>> getAllCountries(
            @RequestParam(required = false) Continent continent) {
        if (continent != null) {
            return ResponseEntity.ok(countryService.getCountriesByContinent(continent));
        }
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CountryResponse> getCountryById(@PathVariable Long id) {
        return ResponseEntity.ok(countryService.getCountryById(id));
    }

    @PostMapping
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(countryService.createCountry(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CountryResponse> updateCountry(@PathVariable Long id,
                                                          @Valid @RequestBody CountryRequest request) {
        return ResponseEntity.ok(countryService.updateCountry(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCountry(@PathVariable Long id) {
        countryService.deleteCountry(id);
        return ResponseEntity.noContent().build();
    }
}
