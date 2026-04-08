package com.travelRec.service;

import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.mapper.CityMapper;
import com.travelRec.repository.CityRepository;
import com.travelRec.repository.RatingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CityService {

    private final CityRepository cityRepository;
    private final RatingRepository ratingRepository;
    private final CountryService countryService;
    private final CityMapper cityMapper;

    public List<CityResponse> getAllCities() {
        return cityRepository.findAll().stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CityResponse getCityById(Long id) {
        City city = findCityOrThrow(id);
        return cityMapper.toResponse(city);
    }

    public List<CityResponse> getCitiesByCountry(Long countryId) {
        return cityRepository.findByCountryId(countryId).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getCitiesByContinent(Continent continent) {
        return cityRepository.findByCountryContinent(continent).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getCitiesByCityType(CityType cityType) {
        return cityRepository.findByCityType(cityType).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getCitiesByClimateType(ClimateType climateType) {
        return cityRepository.findByClimateType(climateType).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> searchCities(String query) {
        return cityRepository.searchByNameOrRegion(query).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getPopularCities(int limit) {
        return cityRepository.findTopByPopularity(limit).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getNearbyCities(Long cityId, double radiusKm) {
        City city = findCityOrThrow(cityId);
        return cityRepository.findNearbyCities(cityId, city.getLatitude(), city.getLongitude(), radiusKm).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<CityResponse> getCitiesInSameCountry(Long cityId) {
        City city = findCityOrThrow(cityId);
        return cityRepository.findByCountryIdExcluding(city.getCountry().getId(), cityId).stream()
                .map(cityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CityResponse createCity(CityRequest request) {
        Country country = countryService.findCountryOrThrow(request.getCountryId());
        City city = cityMapper.toEntity(request, country);
        City saved = cityRepository.save(city);
        return cityMapper.toResponse(saved);
    }

    @Transactional
    public CityResponse updateCity(Long id, CityRequest request) {
        City city = findCityOrThrow(id);
        cityMapper.updateEntity(city, request);
        return cityMapper.toResponse(city);
    }

    @Transactional
    public void deleteCity(Long id) {
        City city = findCityOrThrow(id);
        cityRepository.delete(city);
    }

    @Transactional
    public void recalculateScores(Long cityId) {
        City city = findCityOrThrow(cityId);
        long count = ratingRepository.countByCityId(cityId);

        if (count == 0) return;

        double userWeight = Math.min(count / (count + 10.0), 0.6);
        double baseWeight = 1.0 - userWeight;

        city.setCultureScore(blendScore(city.getBaseCultureScore(),
                ratingRepository.avgCultureRatingByCityId(cityId), baseWeight, userWeight));
        city.setFoodScore(blendScore(city.getBaseFoodScore(),
                ratingRepository.avgFoodRatingByCityId(cityId), baseWeight, userWeight));
        city.setNightlifeScore(blendScore(city.getBaseNightlifeScore(),
                ratingRepository.avgNightlifeRatingByCityId(cityId), baseWeight, userWeight));
        city.setNatureScore(blendScore(city.getBaseNatureScore(),
                ratingRepository.avgNatureRatingByCityId(cityId), baseWeight, userWeight));
        city.setSafetyScore(blendScore(city.getBaseSafetyScore(),
                ratingRepository.avgSafetyRatingByCityId(cityId), baseWeight, userWeight));
        city.setCostLevel(blendScore(city.getBaseCostLevel(),
                ratingRepository.avgCostRatingByCityId(cityId), baseWeight, userWeight));
        city.setBeachScore(blendScore(city.getBaseBeachScore(),
                ratingRepository.avgBeachRatingByCityId(cityId), baseWeight, userWeight));
        city.setArchitectureScore(blendScore(city.getBaseArchitectureScore(),
                ratingRepository.avgArchitectureRatingByCityId(cityId), baseWeight, userWeight));
        city.setShoppingScore(blendScore(city.getBaseShoppingScore(),
                ratingRepository.avgShoppingRatingByCityId(cityId), baseWeight, userWeight));

        city.setRatingCount((int) count);
        cityRepository.save(city);
    }

    private Float blendScore(Float baseScore, java.util.Optional<Double> avgRating, double baseWeight, double userWeight) {
        if (avgRating.isEmpty()) return baseScore;
        double normalizedRating = (avgRating.get() - 1.0) / 4.0;
        return (float) (baseScore * baseWeight + normalizedRating * userWeight);
    }

    public City findCityOrThrow(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));
    }
}
