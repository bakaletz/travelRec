package com.travelRec.service.recommendation;

import com.travelRec.dto.recommendation.RecommendationResponse;
import com.travelRec.dto.recommendation.TripRecommendationResponse;
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
import com.travelRec.repository.TripCityRepository;
import com.travelRec.repository.UserPreferencesRepository;
import com.travelRec.service.recommendation.TripProfile;
import com.travelRec.service.recommendation.TripProfileService;
import com.travelRec.util.ContextDistance;
import com.travelRec.util.VectorMath;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private final TripProfileService tripProfileService;
    private final TripCityRepository tripCityRepository;

    private static final double BASE_LEARNING_RATE = 0.1;
    private static final double QUICK_LEARNING_RATE = 0.04;
    private static final int QUICK_POSITIVE_THRESHOLD = 4;
    private static final double PENALTY_MULTIPLIER = 0.85;
    private static final double MAX_CITY_TYPE_PENALTY = 0.20;
    private static final double MAX_CLIMATE_PENALTY = 0.25;
    private static final double MAX_CONTINENT_PENALTY = 0.30;
    private static final int POSITIVE_RATING_THRESHOLD = 4;
    private static final int SEED_CANDIDATE_POOL_SIZE = 10;
    private static final double NEARBY_SIMILARITY_WEIGHT = 0.6;
    private static final double NEARBY_PROXIMITY_WEIGHT = 0.4;

    private static final double TRIP_RELEVANCE_WEIGHT = 0.65;
    private static final double TRIP_COHERENCE_WEIGHT = 0.35;
    private static final double COHERENCE_CONTEXT_WEIGHT = 0.5;
    private static final double COHERENCE_PROXIMITY_WEIGHT = 0.5;
    private static final double PROXIMITY_SATURATION_KM = 2000.0;
    private static final double WORST_LINK_WEIGHT = 0.3;
    private static final double MIN_TRIP_SCORE = 0.55;
    private static final double COLD_START_MIN_TRIP_SCORE = 0.35;
    private static final double COLD_START_POPULARITY_WEIGHT = 0.7;
    private static final double COLD_START_COHERENCE_FLOOR = 0.5;
    private static final double OVERLAP_PENALTY_PER_CITY = 0.12;
    private static final double COLD_START_DURATION_REPEAT_PENALTY = 0.2;
    private static final int TRIP_RESULT_LIMIT = 4;
    private static final int COLD_START_DURATION_LOW = 3;
    private static final int COLD_START_DURATION_MID = 7;
    private static final int COLD_START_DURATION_HIGH = 14;
    private static final int MIN_DAYS_PER_CITY = 2;
    private static final int SEED_POOL_PER_LENGTH = 12;
    private static final int MIN_TRIP_CITIES = 2;

    public List<RecommendationResponse> getPersonalized(Long userId, int limit,
                                                        List<Continent> continent,
                                                        List<CityType> cityType,
                                                        List<ClimateType> climateType) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        Set<Long> visitedCityIds = new HashSet<>(tripCityRepository.findVisitedCityIdsByUserId(userId));
        List<City> cities = applyFilters(cityRepository.findAllWithCountry(), continent, cityType, climateType).stream()
                .filter(c -> !visitedCityIds.contains(c.getId()))
                .collect(Collectors.toList());

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

    public List<RecommendationResponse> getCountryCityMatches(Long userId, Long countryId) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        double[] userVector = prefs.toVector();

        return cityRepository.findByCountryId(countryId).stream()
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
                .collect(Collectors.toList());
    }

    public RecommendationResponse getMatch(Long userId, Long cityId) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + cityId));

        double[] userVector = prefs.toVector();
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

    public List<TripRecommendationResponse> getRecommendedTrips(Long userId, List<Continent> continent) {
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Preferences not found for user: " + userId));

        TripProfile profile = tripProfileService.buildProfile(userId);
        double[] userVector = prefs.toVector();

        Set<Continent> expanded = expandContinents(continent);
        Set<Long> visitedCityIds = new HashSet<>(tripCityRepository.findVisitedCityIdsByUserId(userId));
        List<City> allCities = cityRepository.findAllWithCountry().stream()
                .filter(c -> !visitedCityIds.contains(c.getId()))
                .filter(c -> expanded == null
                        || (c.getCountry() != null && expanded.contains(c.getCountry().getContinent())))
                .collect(Collectors.toList());

        if (allCities.size() < MIN_TRIP_CITIES) {
            return List.of();
        }

        boolean coldStart = profile.isColdStart();
        double maxPopularity = 0.0;
        for (City c : allCities) {
            if (c.getPopularity() != null && c.getPopularity() > maxPopularity) {
                maxPopularity = c.getPopularity();
            }
        }

        List<TripVariant> variants = coldStart
                ? coldStartVariants()
                : historyVariants(profile);

        final double maxPop = maxPopularity;
        List<City> seeds = allCities.stream()
                .sorted(Comparator.comparingDouble(
                        (City c) -> cityRelevance(userVector, c, profile.dominantCityType(), coldStart, maxPop)).reversed())
                .limit(SEED_POOL_PER_LENGTH)
                .collect(Collectors.toList());

        List<RouteCandidate> built = new ArrayList<>();
        Set<Set<Long>> seenCitySets = new HashSet<>();

        for (TripVariant variant : variants) {
            for (City seed : seeds) {
                List<City> route = assembleRoute(userVector, allCities, seed, variant.cityCount(),
                        profile.dominantCityType());
                if (route.size() < MIN_TRIP_CITIES) continue;

                Set<Long> citySet = route.stream().map(City::getId).collect(Collectors.toSet());
                if (!seenCitySets.add(citySet)) continue;

                route = orderRouteByProximity(route);

                double relevance = meanRouteRelevance(userVector, route, profile.dominantCityType(), coldStart, maxPop);
                double coherence = coherence(route);
                double tripScore = TRIP_RELEVANCE_WEIGHT * relevance + TRIP_COHERENCE_WEIGHT * coherence;

                built.add(new RouteCandidate(route, citySet, tripScore, relevance, coherence, variant));
            }
        }

        double threshold = coldStart ? COLD_START_MIN_TRIP_SCORE : MIN_TRIP_SCORE;

        List<RouteCandidate> pool = built.stream()
                .filter(candidate -> candidate.tripScore() >= threshold)
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            pool = new ArrayList<>(built);
        }

        List<RouteCandidate> selected = selectDiverse(pool, coldStart);

        return selected.stream()
                .map(candidate -> toResponse(candidate, profile))
                .collect(Collectors.toList());
    }

    private List<RouteCandidate> selectDiverse(List<RouteCandidate> pool, boolean coldStart) {
        List<RouteCandidate> remaining = new ArrayList<>(pool);
        List<RouteCandidate> selected = new ArrayList<>(TRIP_RESULT_LIMIT);
        Set<Long> usedCityIds = new HashSet<>();
        Set<Integer> usedDurations = new HashSet<>();

        while (selected.size() < TRIP_RESULT_LIMIT && !remaining.isEmpty()) {
            RouteCandidate best = null;
            double bestAdjusted = Double.NEGATIVE_INFINITY;

            for (RouteCandidate candidate : remaining) {
                int overlap = 0;
                for (Long id : candidate.citySet()) {
                    if (usedCityIds.contains(id)) overlap++;
                }
                double adjusted = candidate.tripScore() - OVERLAP_PENALTY_PER_CITY * overlap;

                if (coldStart && usedDurations.contains(candidate.variant().durationDays())) {
                    adjusted -= COLD_START_DURATION_REPEAT_PENALTY;
                }

                if (adjusted > bestAdjusted) {
                    bestAdjusted = adjusted;
                    best = candidate;
                }
            }

            if (best == null) break;
            selected.add(best);
            usedCityIds.addAll(best.citySet());
            usedDurations.add(best.variant().durationDays());
            remaining.remove(best);
        }

        return selected;
    }

    private TripRecommendationResponse toResponse(RouteCandidate candidate, TripProfile profile) {
        return TripRecommendationResponse.builder()
                .cities(candidate.route().stream().map(cityMapper::toResponse).collect(Collectors.toList()))
                .tripScore(VectorMath.round3(candidate.tripScore()))
                .relevanceScore(VectorMath.round3(candidate.relevance()))
                .coherenceScore(VectorMath.round3(candidate.coherence()))
                .suggestedDurationDays(candidate.variant().durationDays())
                .totalDistanceKm(routeDistanceKm(candidate.route()))
                .dominantCityType(profile.dominantCityType())
                .reason(buildReason(profile, candidate.variant()))
                .build();
    }

    private Double routeDistanceKm(List<City> route) {
        if (route == null || route.size() < 2) return null;
        double total = 0.0;
        for (int i = 1; i < route.size(); i++) {
            City prev = route.get(i - 1);
            City curr = route.get(i);
            total += VectorMath.haversineDistance(prev.getLatitude(), prev.getLongitude(),
                    curr.getLatitude(), curr.getLongitude());
        }
        return (double) Math.round(total);
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

    @Transactional
    public void updatePreferencesFromQuick(User user, Rating rating) {
        if (rating.isDetailed() || rating.getOverallScore() == null) return;
        if (rating.getOverallScore() < QUICK_POSITIVE_THRESHOLD) return;

        UserPreferences prefs = preferencesRepository.findByUserId(user.getId())
                .orElse(null);
        if (prefs == null) return;

        City city = rating.getCity();
        if (city == null) return;

        prefs.setCultureWeight(VectorMath.adaptiveEma(prefs.getCultureWeight(),
                city.getCultureScore(), prefs.getCultureRatingCount(), QUICK_LEARNING_RATE));
        prefs.setCultureRatingCount(prefs.getCultureRatingCount() + 1);

        prefs.setFoodWeight(VectorMath.adaptiveEma(prefs.getFoodWeight(),
                city.getFoodScore(), prefs.getFoodRatingCount(), QUICK_LEARNING_RATE));
        prefs.setFoodRatingCount(prefs.getFoodRatingCount() + 1);

        prefs.setNightlifeWeight(VectorMath.adaptiveEma(prefs.getNightlifeWeight(),
                city.getNightlifeScore(), prefs.getNightlifeRatingCount(), QUICK_LEARNING_RATE));
        prefs.setNightlifeRatingCount(prefs.getNightlifeRatingCount() + 1);

        prefs.setNatureWeight(VectorMath.adaptiveEma(prefs.getNatureWeight(),
                city.getNatureScore(), prefs.getNatureRatingCount(), QUICK_LEARNING_RATE));
        prefs.setNatureRatingCount(prefs.getNatureRatingCount() + 1);

        prefs.setSafetyWeight(VectorMath.adaptiveEma(prefs.getSafetyWeight(),
                city.getSafetyScore(), prefs.getSafetyRatingCount(), QUICK_LEARNING_RATE));
        prefs.setSafetyRatingCount(prefs.getSafetyRatingCount() + 1);

        prefs.setBudgetWeight(VectorMath.adaptiveEma(prefs.getBudgetWeight(),
                city.getCostLevel(), prefs.getBudgetRatingCount(), QUICK_LEARNING_RATE));
        prefs.setBudgetRatingCount(prefs.getBudgetRatingCount() + 1);

        prefs.setBeachWeight(VectorMath.adaptiveEma(prefs.getBeachWeight(),
                city.getBeachScore(), prefs.getBeachRatingCount(), QUICK_LEARNING_RATE));
        prefs.setBeachRatingCount(prefs.getBeachRatingCount() + 1);

        prefs.setArchitectureWeight(VectorMath.adaptiveEma(prefs.getArchitectureWeight(),
                city.getArchitectureScore(), prefs.getArchitectureRatingCount(), QUICK_LEARNING_RATE));
        prefs.setArchitectureRatingCount(prefs.getArchitectureRatingCount() + 1);

        prefs.setShoppingWeight(VectorMath.adaptiveEma(prefs.getShoppingWeight(),
                city.getShoppingScore(), prefs.getShoppingRatingCount(), QUICK_LEARNING_RATE));
        prefs.setShoppingRatingCount(prefs.getShoppingRatingCount() + 1);
    }

    private List<TripVariant> historyVariants(TripProfile profile) {
        int m = profile.targetCityCount();
        int duration = profile.targetDurationDays();
        int half = profile.durationWindowHalf();

        int[] counts = {
                Math.max(MIN_TRIP_CITIES, m - 1),
                Math.max(MIN_TRIP_CITIES, m),
                Math.max(MIN_TRIP_CITIES, m),
                Math.min(4, Math.max(MIN_TRIP_CITIES, m + 1))
        };
        int[] durations = {profile.minDurationDays(), duration, duration, profile.maxDurationDays()};

        List<TripVariant> variants = new ArrayList<>(counts.length);
        for (int i = 0; i < counts.length; i++) {
            int minDays = counts[i] * MIN_DAYS_PER_CITY;
            int days = Math.max(durations[i], minDays);
            variants.add(new TripVariant(counts[i], days));
        }
        return variants;
    }

    private List<TripVariant> coldStartVariants() {
        return List.of(
                new TripVariant(2, COLD_START_DURATION_LOW),
                new TripVariant(3, COLD_START_DURATION_MID),
                new TripVariant(4, COLD_START_DURATION_HIGH)
        );
    }

    private List<City> assembleRoute(double[] userVector, List<City> candidates, City seed,
                                     int cityCount, CityType dominantType) {
        if (candidates.isEmpty() || seed == null || cityCount < MIN_TRIP_CITIES) return List.of();

        if (cityCount == 2) {
            return bestPairFrom(userVector, candidates, seed, dominantType);
        }

        return greedyRoute(userVector, candidates, seed, cityCount, dominantType);
    }

    private List<City> bestPairFrom(double[] userVector, List<City> candidates, City seed, CityType dominantType) {
        double matchSeed = biasedMatch(userVector, seed, dominantType);

        City bestPartner = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (City candidate : candidates) {
            if (candidate.getId().equals(seed.getId())) continue;

            double matchCandidate = biasedMatch(userVector, candidate, dominantType);
            double relevance = (matchSeed + matchCandidate) / 2.0;
            double coherence = pairCoherence(seed, candidate);
            double score = TRIP_RELEVANCE_WEIGHT * relevance + TRIP_COHERENCE_WEIGHT * coherence;
            if (score > bestScore) {
                bestScore = score;
                bestPartner = candidate;
            }
        }

        if (bestPartner == null) return List.of();
        List<City> pair = new ArrayList<>(2);
        pair.add(seed);
        pair.add(bestPartner);
        return pair;
    }

    private List<City> greedyRoute(double[] userVector, List<City> candidates, City seed,
                                   int cityCount, CityType dominantType) {
        List<City> route = new ArrayList<>(cityCount);
        route.add(seed);
        Set<Long> used = new LinkedHashSet<>();
        used.add(seed.getId());

        while (route.size() < cityCount) {
            City best = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (City candidate : candidates) {
                if (used.contains(candidate.getId())) continue;

                double match = biasedMatch(userVector, candidate, dominantType);
                double coherenceWithRoute = 0.0;
                for (City inRoute : route) {
                    coherenceWithRoute += pairCoherence(candidate, inRoute);
                }
                coherenceWithRoute /= route.size();

                double score = TRIP_RELEVANCE_WEIGHT * match + TRIP_COHERENCE_WEIGHT * coherenceWithRoute;
                if (score > bestScore) {
                    bestScore = score;
                    best = candidate;
                }
            }

            if (best == null) break;
            route.add(best);
            used.add(best.getId());
        }

        return route;
    }

    private double meanRouteRelevance(double[] userVector, List<City> route, CityType dominantType,
                                      boolean coldStart, double maxPopularity) {
        if (route.isEmpty()) return 0.0;
        double sum = 0.0;
        for (City city : route) {
            sum += cityRelevance(userVector, city, dominantType, coldStart, maxPopularity);
        }
        return sum / route.size();
    }

    private double cityRelevance(double[] userVector, City city, CityType dominantType,
                                 boolean coldStart, double maxPopularity) {
        if (!coldStart) {
            return biasedMatch(userVector, city, dominantType);
        }

        double popularity = maxPopularity > 0.0 && city.getPopularity() != null
                ? city.getPopularity() / maxPopularity
                : 0.0;

        return COLD_START_POPULARITY_WEIGHT * popularity
                + (1.0 - COLD_START_POPULARITY_WEIGHT) * COLD_START_COHERENCE_FLOOR;
    }

    private double biasedMatch(double[] userVector, City city, CityType dominantType) {
        double match = VectorMath.centeredCosineSimilarity(userVector, city.toVector());
        if (dominantType != null) {
            double typeDist = ContextDistance.cityTypeDistance(dominantType, city.getCityType());
            match *= (1.0 - MAX_CITY_TYPE_PENALTY * typeDist);
        }
        return match;
    }

    private List<City> orderRouteByProximity(List<City> route) {
        if (route.size() < 3) return route;

        List<City> remaining = new ArrayList<>(route);
        List<City> ordered = new ArrayList<>(route.size());
        ordered.add(remaining.remove(0));

        while (!remaining.isEmpty()) {
            City last = ordered.get(ordered.size() - 1);
            City nearest = null;
            double minDist = Double.MAX_VALUE;

            for (City candidate : remaining) {
                double dist = VectorMath.haversineDistance(
                        last.getLatitude(), last.getLongitude(),
                        candidate.getLatitude(), candidate.getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = candidate;
                }
            }

            remaining.remove(nearest);
            ordered.add(nearest);
        }

        return ordered;
    }

    private double coherence(List<City> route) {
        if (route.size() < 2) return 1.0;

        double sum = 0.0;
        double worst = Double.POSITIVE_INFINITY;
        int transitions = 0;
        for (int i = 1; i < route.size(); i++) {
            double link = pairCoherence(route.get(i - 1), route.get(i));
            sum += link;
            if (link < worst) worst = link;
            transitions++;
        }

        if (transitions == 0) return 1.0;
        double mean = sum / transitions;
        return (1.0 - WORST_LINK_WEIGHT) * mean + WORST_LINK_WEIGHT * worst;
    }

    private double pairCoherence(City a, City b) {
        double cityTypeDist = ContextDistance.cityTypeDistance(a.getCityType(), b.getCityType());
        double climateDist = ContextDistance.climateDistance(a.getClimateType(), b.getClimateType());

        double continentDist = 0.0;
        if (a.getCountry() != null && b.getCountry() != null) {
            continentDist = ContextDistance.continentDistance(
                    a.getCountry().getContinent(), b.getCountry().getContinent());
        }

        double contextDist = (cityTypeDist + climateDist + continentDist) / 3.0;
        double contextCoherence = 1.0 - contextDist;

        double distance = VectorMath.haversineDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude());
        double proximity = Math.max(0.0, 1.0 - (distance / PROXIMITY_SATURATION_KM));

        return COHERENCE_CONTEXT_WEIGHT * contextCoherence + COHERENCE_PROXIMITY_WEIGHT * proximity;
    }

    private String buildReason(TripProfile profile, TripVariant variant) {
        if (profile.isColdStart()) {
            return "A " + variant.durationDays() + "-day trip to get you started";
        }
        return "Based on your usual " + profile.targetDurationDays() + "-day trips";
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

    private Set<Continent> expandContinents(List<Continent> continents) {
        if (continents == null || continents.isEmpty()) return null;

        Set<Continent> expanded = new HashSet<>(continents);
        for (Continent c : continents) {
            switch (c) {
                case EUROPE -> expanded.add(Continent.EUROPE_ASIA);
                case ASIA -> {
                    expanded.add(Continent.EUROPE_ASIA);
                    expanded.add(Continent.AFRICA_ASIA);
                }
                case AFRICA -> expanded.add(Continent.AFRICA_ASIA);
                default -> { }
            }
        }
        return expanded;
    }

    private record TripVariant(int cityCount, int durationDays) {}

    private record RouteCandidate(
            List<City> route,
            Set<Long> citySet,
            double tripScore,
            double relevance,
            double coherence,
            TripVariant variant
    ) {}
}