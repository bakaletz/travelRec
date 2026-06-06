package com.travelRec.service.recommendation;

import com.travelRec.entity.City;
import com.travelRec.entity.Rating;
import com.travelRec.entity.Trip;
import com.travelRec.entity.TripCity;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.repository.TripRepository;
import com.travelRec.util.VectorMath;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripProfileService {

    private final TripRepository tripRepository;

    private static final int SPARSE_THRESHOLD = 2;
    private static final int RICH_THRESHOLD = 4;

    private static final int DEFAULT_DURATION_DAYS = 7;
    private static final int DEFAULT_CITY_COUNT = 2;
    private static final int MAX_CITY_COUNT = 4;

    private static final int COLD_START_WINDOW_HALF = 6;
    private static final int SPARSE_WINDOW_HALF = 3;
    private static final int RICH_WINDOW_HALF = 1;

    private static final double NEUTRAL_TRIP_WEIGHT = 1.0;

    private static final double DEFAULT_SATURATION_KM = 2000.0;
    private static final double MIN_SATURATION_KM = 500.0;
    private static final double MAX_SATURATION_KM = 5000.0;
    private static final double SATURATION_LEG_MULTIPLIER = 2.0;

    public TripProfile buildProfile(Long userId) {
        List<Trip> trips = loadValidTrips(userId);
        int count = trips.size();

        if (count < SPARSE_THRESHOLD) {
            return new TripProfile(
                    DEFAULT_CITY_COUNT,
                    DEFAULT_DURATION_DAYS,
                    COLD_START_WINDOW_HALF,
                    null,
                    null,
                    DEFAULT_SATURATION_KM,
                    TripProfile.DataBucket.COLD_START);
        }

        int medianCityCount = medianCityCount(trips);
        int medianDuration = medianDurationDays(trips);
        CityType dominantType = dominantCityType(trips);
        ClimateType dominantClimate = dominantClimateType(trips);
        double saturation = proximitySaturation(trips);

        TripProfile.DataBucket bucket = count >= RICH_THRESHOLD
                ? TripProfile.DataBucket.RICH
                : TripProfile.DataBucket.SPARSE;

        int windowHalf = bucket == TripProfile.DataBucket.RICH
                ? RICH_WINDOW_HALF
                : SPARSE_WINDOW_HALF;

        int cityCount = Math.min(MAX_CITY_COUNT, Math.max(1, medianCityCount));
        int duration = medianDuration > 0 ? medianDuration : DEFAULT_DURATION_DAYS;

        return new TripProfile(cityCount, duration, windowHalf, dominantType, dominantClimate, saturation, bucket);
    }

    private List<Trip> loadValidTrips(Long userId) {
        List<Trip> completed = tripRepository.findByUserIdAndStatus(userId, TripStatus.COMPLETED);
        List<Trip> rated = tripRepository.findByUserIdAndStatus(userId, TripStatus.RATED);

        List<Trip> valid = new ArrayList<>(completed.size() + rated.size());
        for (Trip trip : completed) {
            if (!trip.getTripCities().isEmpty()) valid.add(trip);
        }
        for (Trip trip : rated) {
            if (!trip.getTripCities().isEmpty()) valid.add(trip);
        }
        return valid;
    }

    private int medianCityCount(List<Trip> trips) {
        List<Integer> counts = new ArrayList<>(trips.size());
        for (Trip trip : trips) {
            counts.add(trip.getTripCities().size());
        }
        return medianInt(counts);
    }

    private int medianDurationDays(List<Trip> trips) {
        List<Integer> durations = new ArrayList<>();
        for (Trip trip : trips) {
            if (trip.getStartDate() != null && trip.getEndDate() != null) {
                long days = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
                if (days > 0) durations.add((int) days);
            }
        }
        return durations.isEmpty() ? 0 : medianInt(durations);
    }

    private CityType dominantCityType(List<Trip> trips) {
        Map<CityType, Double> weightedFrequency = new EnumMap<>(CityType.class);
        for (Trip trip : trips) {
            Map<Long, Double> cityWeights = cityRatingWeights(trip);
            for (TripCity tripCity : trip.getTripCities()) {
                City city = tripCity.getCity();
                if (city == null || city.getCityType() == null) continue;
                double weight = cityWeights.getOrDefault(city.getId(), NEUTRAL_TRIP_WEIGHT);
                weightedFrequency.merge(city.getCityType(), weight, Double::sum);
            }
        }

        CityType dominant = null;
        double best = -1.0;
        for (Map.Entry<CityType, Double> entry : weightedFrequency.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    private Map<Long, Double> cityRatingWeights(Trip trip) {
        Map<Long, Double> weights = new HashMap<>();
        for (Rating rating : trip.getRatings()) {
            if (rating.getCity() == null || rating.getOverallScore() == null) continue;
            weights.put(rating.getCity().getId(), 0.5 + Rating.normalize(rating.getOverallScore()));
        }
        return weights;
    }

    private ClimateType dominantClimateType(List<Trip> trips) {
        Map<ClimateType, Double> weightedFrequency = new EnumMap<>(ClimateType.class);
        for (Trip trip : trips) {
            Map<Long, Double> cityWeights = cityRatingWeights(trip);
            for (TripCity tripCity : trip.getTripCities()) {
                City city = tripCity.getCity();
                if (city == null || city.getClimateType() == null) continue;
                double weight = cityWeights.getOrDefault(city.getId(), NEUTRAL_TRIP_WEIGHT);
                weightedFrequency.merge(city.getClimateType(), weight, Double::sum);
            }
        }

        ClimateType dominant = null;
        double best = -1.0;
        for (Map.Entry<ClimateType, Double> entry : weightedFrequency.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
    }

    private double proximitySaturation(List<Trip> trips) {
        double medianLeg = medianLegDistanceKm(trips);
        if (medianLeg <= 0.0) return DEFAULT_SATURATION_KM;
        double saturation = medianLeg * SATURATION_LEG_MULTIPLIER;
        return Math.max(MIN_SATURATION_KM, Math.min(MAX_SATURATION_KM, saturation));
    }

    private double medianLegDistanceKm(List<Trip> trips) {
        List<Double> legs = new ArrayList<>();
        for (Trip trip : trips) {
            List<TripCity> cities = trip.getTripCities();
            for (int i = 1; i < cities.size(); i++) {
                City a = cities.get(i - 1).getCity();
                City b = cities.get(i).getCity();
                if (a == null || b == null) continue;
                if (a.getLatitude() == null || a.getLongitude() == null
                        || b.getLatitude() == null || b.getLongitude() == null) continue;
                legs.add(VectorMath.haversineDistance(
                        a.getLatitude(), a.getLongitude(),
                        b.getLatitude(), b.getLongitude()));
            }
        }
        if (legs.isEmpty()) return 0.0;
        Collections.sort(legs);
        int mid = legs.size() / 2;
        if (legs.size() % 2 == 1) {
            return legs.get(mid);
        }
        return (legs.get(mid - 1) + legs.get(mid)) / 2.0;
    }

    private int medianInt(List<Integer> values) {
        if (values.isEmpty()) return 0;
        Collections.sort(values);
        int mid = values.size() / 2;
        if (values.size() % 2 == 1) {
            return values.get(mid);
        }
        return (int) Math.round((values.get(mid - 1) + values.get(mid)) / 2.0);
    }
}