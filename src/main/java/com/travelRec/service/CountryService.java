package com.travelRec.service;

import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.Continent;
import com.travelRec.mapper.CountryMapper;
import com.travelRec.repository.CountryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CountryService {

    private final CountryRepository countryRepository;
    private final CountryMapper countryMapper;

    public List<CountryResponse> getAllCountries() {
        return countryRepository.findAll().stream()
                .map(countryMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CountryResponse getCountryById(Long id) {
        Country country = findCountryOrThrow(id);
        return countryMapper.toResponse(country);
    }

    public List<CountryResponse> getCountriesByContinent(Continent continent) {
        return countryRepository.findByContinent(continent).stream()
                .map(countryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CountryResponse createCountry(CountryRequest request) {
        if (countryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Country with name '" + request.getName() + "' already exists");
        }
        if (countryRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Country with code '" + request.getCode() + "' already exists");
        }

        Country country = countryMapper.toEntity(request);
        Country saved = countryRepository.save(country);
        return countryMapper.toResponse(saved);
    }

    @Transactional
    public CountryResponse updateCountry(Long id, CountryRequest request) {
        Country country = findCountryOrThrow(id);
        countryMapper.updateEntity(country, request);
        return countryMapper.toResponse(country);
    }

    @Transactional
    public void deleteCountry(Long id) {
        Country country = findCountryOrThrow(id);
        countryRepository.delete(country);
    }

    public Country findCountryOrThrow(Long id) {
        return countryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Country not found with id: " + id));
    }
}
