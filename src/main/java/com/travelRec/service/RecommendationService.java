package com.travelRec.service;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Rating;
import com.travelRec.entity.User;
import com.travelRec.entity.UserPreferences;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.mapper.CityMapper;
import com.travelRec.repository.CityRepository;
import com.travelRec.repository.UserPreferencesRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final CityRepository cityRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final CityMapper cityMapper;

    private static final double LEARNING_RATE = 0.1;

    public List<RecommendationResponse> getPersonalized(Long userId, int limit,
                                                         Continent continent,
                                                         CityType cityType,
                                                         ClimateType climateType) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        List<City> cities = cityRepository.findAll();

        cities = applyFilters(cities, continent, cityType, climateType, prefs);

        double[] userVector = prefs.toVector();

        return cities.stream()
                .map(city -> {
                    double score = cosineSimilarity(userVector, city.toVector());
                    return RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(Math.round(score * 1000.0) / 1000.0)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getSimilarityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<RecommendationResponse> getPopular(int limit) {
        return cityRepository.findTopByPopularity(limit).stream()
                .map(city -> RecommendationResponse.builder()
                        .city(cityMapper.toResponse(city))
                        .similarityScore(null)
                        .build())
                .collect(Collectors.toList());
    }

    public List<RecommendationResponse> getNearbyRecommendations(Long userId, Long cityId, double radiusKm, int limit) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        City origin = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        List<City> nearby = cityRepository.findNearbyCities(cityId, origin.getLatitude(), origin.getLongitude(), radiusKm);

        double[] userVector = prefs.toVector();

        return nearby.stream()
                .map(city -> {
                    double similarity = cosineSimilarity(userVector, city.toVector());
                    double distance = haversineDistance(origin.getLatitude(), origin.getLongitude(),
                            city.getLatitude(), city.getLongitude());
                    double proximityScore = 1.0 - (distance / radiusKm);
                    double finalScore = similarity * 0.6 + proximityScore * 0.4;

                    return RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(Math.round(finalScore * 1000.0) / 1000.0)
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getSimilarityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePreferences(User user, Rating rating) {
        UserPreferences prefs = preferencesRepository.findByUserId(user.getId())
                .orElse(null);

        if (prefs == null || !rating.isDetailed()) return;

        City city = rating.getCity();
        double[] cityVector = city.toVector();

        if (rating.getCultureRating() != null) {
            prefs.setCultureWeight(adjustWeight(prefs.getCultureWeight(),
                    Rating.normalize(rating.getCultureRating()), cityVector[0]));
        }
        if (rating.getFoodRating() != null) {
            prefs.setFoodWeight(adjustWeight(prefs.getFoodWeight(),
                    Rating.normalize(rating.getFoodRating()), cityVector[1]));
        }
        if (rating.getNightlifeRating() != null) {
            prefs.setNightlifeWeight(adjustWeight(prefs.getNightlifeWeight(),
                    Rating.normalize(rating.getNightlifeRating()), cityVector[2]));
        }
        if (rating.getNatureRating() != null) {
            prefs.setNatureWeight(adjustWeight(prefs.getNatureWeight(),
                    Rating.normalize(rating.getNatureRating()), cityVector[3]));
        }
        if (rating.getSafetyRating() != null) {
            prefs.setSafetyWeight(adjustWeight(prefs.getSafetyWeight(),
                    Rating.normalize(rating.getSafetyRating()), cityVector[4]));
        }
        if (rating.getCostRating() != null) {
            prefs.setBudgetWeight(adjustWeight(prefs.getBudgetWeight(),
                    Rating.normalize(rating.getCostRating()), cityVector[5]));
        }
        if (rating.getBeachRating() != null) {
            prefs.setBeachWeight(adjustWeight(prefs.getBeachWeight(),
                    Rating.normalize(rating.getBeachRating()), cityVector[6]));
        }
        if (rating.getArchitectureRating() != null) {
            prefs.setArchitectureWeight(adjustWeight(prefs.getArchitectureWeight(),
                    Rating.normalize(rating.getArchitectureRating()), cityVector[7]));
        }
        if (rating.getShoppingRating() != null) {
            prefs.setShoppingWeight(adjustWeight(prefs.getShoppingWeight(),
                    Rating.normalize(rating.getShoppingRating()), cityVector[8]));
        }
    }

    private float adjustWeight(float currentWeight, double normalizedRating, double cityAttribute) {
        double expected = currentWeight * cityAttribute;
        double newWeight = currentWeight + LEARNING_RATE * (normalizedRating - expected) * cityAttribute;
        return (float) Math.max(0.0, Math.min(1.0, newWeight));
    }

    public static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vectors must have same length");

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private List<City> applyFilters(List<City> cities, Continent continent,
                                     CityType cityType, ClimateType climateType,
                                     UserPreferences prefs) {
        if (continent != null) {
            cities = cities.stream()
                    .filter(c -> c.getCountry().getContinent() == continent)
                    .collect(Collectors.toList());
        }

        CityType filterCityType = cityType != null ? cityType : prefs.getPreferredCityType();
        if (filterCityType != null) {
            cities = cities.stream()
                    .filter(c -> c.getCityType() == filterCityType)
                    .collect(Collectors.toList());
        }

        ClimateType filterClimate = climateType != null ? climateType : prefs.getPreferredClimate();
        if (filterClimate != null) {
            cities = cities.stream()
                    .filter(c -> c.getClimateType() == filterClimate)
                    .collect(Collectors.toList());
        }

        return cities;
    }
}
