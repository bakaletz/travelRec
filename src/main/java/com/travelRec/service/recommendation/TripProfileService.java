package com.travelRec.service.recommendation;

import com.travelRec.entity.City;
import com.travelRec.entity.Trip;
import com.travelRec.entity.TripCity;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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

    public TripProfile buildProfile(Long userId) {
        List<Trip> trips = loadValidTrips(userId);
        int count = trips.size();

        if (count < SPARSE_THRESHOLD) {
            return new TripProfile(
                    DEFAULT_CITY_COUNT,
                    DEFAULT_DURATION_DAYS,
                    COLD_START_WINDOW_HALF,
                    null,
                    TripProfile.DataBucket.COLD_START);
        }

        int medianCityCount = medianCityCount(trips);
        int medianDuration = medianDurationDays(trips);
        CityType dominantType = dominantCityType(trips);

        TripProfile.DataBucket bucket = count >= RICH_THRESHOLD
                ? TripProfile.DataBucket.RICH
                : TripProfile.DataBucket.SPARSE;

        int windowHalf = bucket == TripProfile.DataBucket.RICH
                ? RICH_WINDOW_HALF
                : SPARSE_WINDOW_HALF;

        int cityCount = Math.min(MAX_CITY_COUNT, Math.max(1, medianCityCount));
        int duration = medianDuration > 0 ? medianDuration : DEFAULT_DURATION_DAYS;

        return new TripProfile(cityCount, duration, windowHalf, dominantType, bucket);
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
        Map<CityType, Integer> frequency = new EnumMap<>(CityType.class);
        for (Trip trip : trips) {
            for (TripCity tripCity : trip.getTripCities()) {
                City city = tripCity.getCity();
                if (city == null || city.getCityType() == null) continue;
                frequency.merge(city.getCityType(), 1, Integer::sum);
            }
        }

        CityType dominant = null;
        int best = -1;
        for (Map.Entry<CityType, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > best) {
                best = entry.getValue();
                dominant = entry.getKey();
            }
        }
        return dominant;
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