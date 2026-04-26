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
import com.travelRec.repository.RatingRepository;
import com.travelRec.repository.UserPreferencesRepository;
import com.travelRec.util.ContextDistance;
import com.travelRec.util.VectorMath;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final CityRepository cityRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final CityMapper cityMapper;
    private final RatingRepository ratingRepository;

    private static final double BASE_LEARNING_RATE = 0.1;
    private static final double PENALTY_MULTIPLIER = 0.85;
    private static final double MAX_CITY_TYPE_PENALTY = 0.20;
    private static final double MAX_CLIMATE_PENALTY = 0.25;
    private static final double MAX_CONTINENT_PENALTY = 0.30;
    private static final int POSITIVE_RATING_THRESHOLD = 4;
    private static final int SEED_CANDIDATE_POOL_SIZE = 10;
    private static final double NEARBY_SIMILARITY_WEIGHT = 0.6;
    private static final double NEARBY_PROXIMITY_WEIGHT = 0.4;

    public List<RecommendationResponse> getPersonalized(Long userId, int limit,
                                                        List<Continent> continent,
                                                        List<CityType> cityType,
                                                        List<ClimateType> climateType) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        List<City> cities = applyFilters(cityRepository.findAllWithCountry(), continent, cityType, climateType);

        double[] userVector = prefs.toVector();

        return cities.stream()
                .map(city -> {
                    double score = VectorMath.centeredCosineSimilarity(userVector, city.toVector());

                    if (prefs.hasPreferredCityTypes() && !prefs.matchesCityType(city.getCityType())) {
                        score *= PENALTY_MULTIPLIER;
                    }
                    if (prefs.hasPreferredClimateTypes() && !prefs.matchesClimateType(city.getClimateType())) {
                        score *= PENALTY_MULTIPLIER;
                    }

                    return RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(VectorMath.round3(score))
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

    public List<RecommendationResponse> getSimilarCities(Long cityId, int limit) {
        City origin = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        return computeSimilarTo(origin, Set.of(origin.getId()), limit, null);
    }

    public List<RecommendationResponse> getBecauseYouLiked(Long userId, int limit) {
        List<Rating> positiveRatings = ratingRepository.findRecentPositiveByUserIdWithCity(
                userId, POSITIVE_RATING_THRESHOLD, PageRequest.of(0, SEED_CANDIDATE_POOL_SIZE));

        if (positiveRatings.isEmpty()) {
            return List.of();
        }

        Rating seedRating = pickWeightedRandomSeed(positiveRatings);
        City seed = seedRating.getCity();

        Set<Long> excludeCityIds = new HashSet<>(ratingRepository.findRatedCityIdsByUserId(userId));
        excludeCityIds.add(seed.getId());

        String reason = "Because you liked " + seed.getName();
        return computeSimilarTo(seed, excludeCityIds, limit, reason);
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
                    double similarity = VectorMath.centeredCosineSimilarity(userVector, city.toVector());
                    double distance = VectorMath.haversineDistance(origin.getLatitude(), origin.getLongitude(),
                            city.getLatitude(), city.getLongitude());
                    double proximityScore = Math.max(0.0, 1.0 - (distance / radiusKm));
                    double finalScore = similarity * NEARBY_SIMILARITY_WEIGHT + proximityScore * NEARBY_PROXIMITY_WEIGHT;

                    return RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(VectorMath.round3(finalScore))
                            .distanceKm(VectorMath.round3(distance))
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getSimilarityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<RecommendationResponse> getNearbyByCoordinates(Long userId, double lat, double lng, double radiusKm, int limit) {
        List<City> nearby = cityRepository.findNearbyCitiesByCoordinates(lat, lng, radiusKm);

        double[] userVector = userId != null
                ? preferencesRepository.findByUserId(userId).map(UserPreferences::toVector).orElse(null)
                : null;

        return nearby.stream()
                .map(city -> {
                    double distance = VectorMath.haversineDistance(lat, lng, city.getLatitude(), city.getLongitude());
                    double proximityScore = Math.max(0.0, 1.0 - (distance / radiusKm));

                    double finalScore;
                    if (userVector != null) {
                        double similarity = VectorMath.centeredCosineSimilarity(userVector, city.toVector());
                        finalScore = similarity * NEARBY_SIMILARITY_WEIGHT + proximityScore * NEARBY_PROXIMITY_WEIGHT;
                    } else {
                        finalScore = proximityScore;
                    }

                    return RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(VectorMath.round3(finalScore))
                            .distanceKm(VectorMath.round3(distance))
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

        if (rating.getCultureRating() != null) {
            prefs.setCultureWeight(VectorMath.adaptiveEma(prefs.getCultureWeight(),
                    Rating.normalize(rating.getCultureRating()), prefs.getCultureRatingCount(), BASE_LEARNING_RATE));
            prefs.setCultureRatingCount(prefs.getCultureRatingCount() + 1);
        }
        if (rating.getFoodRating() != null) {
            prefs.setFoodWeight(VectorMath.adaptiveEma(prefs.getFoodWeight(),
                    Rating.normalize(rating.getFoodRating()), prefs.getFoodRatingCount(), BASE_LEARNING_RATE));
            prefs.setFoodRatingCount(prefs.getFoodRatingCount() + 1);
        }
        if (rating.getNightlifeRating() != null) {
            prefs.setNightlifeWeight(VectorMath.adaptiveEma(prefs.getNightlifeWeight(),
                    Rating.normalize(rating.getNightlifeRating()), prefs.getNightlifeRatingCount(), BASE_LEARNING_RATE));
            prefs.setNightlifeRatingCount(prefs.getNightlifeRatingCount() + 1);
        }
        if (rating.getNatureRating() != null) {
            prefs.setNatureWeight(VectorMath.adaptiveEma(prefs.getNatureWeight(),
                    Rating.normalize(rating.getNatureRating()), prefs.getNatureRatingCount(), BASE_LEARNING_RATE));
            prefs.setNatureRatingCount(prefs.getNatureRatingCount() + 1);
        }
        if (rating.getSafetyRating() != null) {
            prefs.setSafetyWeight(VectorMath.adaptiveEma(prefs.getSafetyWeight(),
                    Rating.normalize(rating.getSafetyRating()), prefs.getSafetyRatingCount(), BASE_LEARNING_RATE));
            prefs.setSafetyRatingCount(prefs.getSafetyRatingCount() + 1);
        }
        if (rating.getCostRating() != null) {
            prefs.setBudgetWeight(VectorMath.adaptiveEma(prefs.getBudgetWeight(),
                    Rating.normalize(rating.getCostRating()), prefs.getBudgetRatingCount(), BASE_LEARNING_RATE));
            prefs.setBudgetRatingCount(prefs.getBudgetRatingCount() + 1);
        }
        if (rating.getBeachRating() != null) {
            prefs.setBeachWeight(VectorMath.adaptiveEma(prefs.getBeachWeight(),
                    Rating.normalize(rating.getBeachRating()), prefs.getBeachRatingCount(), BASE_LEARNING_RATE));
            prefs.setBeachRatingCount(prefs.getBeachRatingCount() + 1);
        }
        if (rating.getArchitectureRating() != null) {
            prefs.setArchitectureWeight(VectorMath.adaptiveEma(prefs.getArchitectureWeight(),
                    Rating.normalize(rating.getArchitectureRating()), prefs.getArchitectureRatingCount(), BASE_LEARNING_RATE));
            prefs.setArchitectureRatingCount(prefs.getArchitectureRatingCount() + 1);
        }
        if (rating.getShoppingRating() != null) {
            prefs.setShoppingWeight(VectorMath.adaptiveEma(prefs.getShoppingWeight(),
                    Rating.normalize(rating.getShoppingRating()), prefs.getShoppingRatingCount(), BASE_LEARNING_RATE));
            prefs.setShoppingRatingCount(prefs.getShoppingRatingCount() + 1);
        }
    }

    private List<RecommendationResponse> computeSimilarTo(City seed, Set<Long> excludeIds, int limit, String reason) {
        double[] seedVector = seed.toVector();

        return cityRepository.findAllWithCountry().stream()
                .filter(city -> !excludeIds.contains(city.getId()))
                .map(city -> {
                    double similarity = VectorMath.centeredCosineSimilarity(seedVector, city.toVector());
                    similarity = applyContextPenalties(similarity, seed, city);
                    RecommendationResponse.RecommendationResponseBuilder builder = RecommendationResponse.builder()
                            .city(cityMapper.toResponse(city))
                            .similarityScore(VectorMath.round3(similarity));
                    if (reason != null) {
                        builder.reason(reason);
                    }
                    return builder.build();
                })
                .sorted(Comparator.comparingDouble(RecommendationResponse::getSimilarityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Rating pickWeightedRandomSeed(List<Rating> candidates) {
        double totalWeight = 0.0;
        for (Rating r : candidates) {
            totalWeight += r.getOverallScore();
        }

        double pick = ThreadLocalRandom.current().nextDouble() * totalWeight;
        double running = 0.0;
        for (Rating r : candidates) {
            running += r.getOverallScore();
            if (running >= pick) {
                return r;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private double applyContextPenalties(double score, City seed, City candidate) {
        double cityTypeDist = ContextDistance.cityTypeDistance(seed.getCityType(), candidate.getCityType());
        score *= (1.0 - MAX_CITY_TYPE_PENALTY * cityTypeDist);

        double climateDist = ContextDistance.climateDistance(seed.getClimateType(), candidate.getClimateType());
        score *= (1.0 - MAX_CLIMATE_PENALTY * climateDist);

        if (seed.getCountry() != null && candidate.getCountry() != null) {
            double continentDist = ContextDistance.continentDistance(
                    seed.getCountry().getContinent(), candidate.getCountry().getContinent());
            score *= (1.0 - MAX_CONTINENT_PENALTY * continentDist);
        }

        return score;
    }

    private List<City> applyFilters(List<City> cities, List<Continent> continents,
                                    List<CityType> cityTypes, List<ClimateType> climateTypes) {
        if (continents != null && !continents.isEmpty()) {
            cities = cities.stream()
                    .filter(c -> continents.contains(c.getCountry().getContinent()))
                    .collect(Collectors.toList());
        }

        if (cityTypes != null && !cityTypes.isEmpty()) {
            cities = cities.stream()
                    .filter(c -> cityTypes.contains(c.getCityType()))
                    .collect(Collectors.toList());
        }

        if (climateTypes != null && !climateTypes.isEmpty()) {
            cities = cities.stream()
                    .filter(c -> climateTypes.contains(c.getClimateType()))
                    .collect(Collectors.toList());
        }
        return cities;
    }
}