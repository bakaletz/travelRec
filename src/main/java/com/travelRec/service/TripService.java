package com.travelRec.service;

import com.travelRec.dto.trip.AddCityToTripRequest;
import com.travelRec.dto.trip.TripCityResponse;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Trip;
import com.travelRec.entity.TripCity;
import com.travelRec.entity.User;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.mapper.TripMapper;
import com.travelRec.repository.TripCityRepository;
import com.travelRec.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripService {

    private final TripRepository tripRepository;
    private final TripCityRepository tripCityRepository;
    private final UserService userService;
    private final CityService cityService;
    private final TripMapper tripMapper;

    public List<TripResponse> getUserTrips(Long userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public List<TripResponse> getUserTripsByStatus(Long userId, TripStatus status) {
        return tripRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    public TripResponse getTripById(Long userId, Long id) {
        Trip trip = findOwnedTrip(userId, id);
        return buildResponse(trip);
    }

    @Transactional
    public TripResponse createTrip(Long userId, TripRequest request) {
        User user = userService.findUserOrThrow(userId);
        Trip trip = tripMapper.toEntity(request, user);
        Trip saved = tripRepository.save(trip);
        return buildResponse(saved);
    }

    @Transactional
    public TripResponse updateTrip(Long userId, Long id, TripRequest request) {
        Trip trip = findOwnedTrip(userId, id);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED trips can be edited");
        }
        tripMapper.updateEntity(trip, request);
        return buildResponse(trip);
    }

    @Transactional
    public TripResponse completeTrip(Long userId, Long id) {
        Trip trip = findOwnedTrip(userId, id);
        trip.complete();
        return buildResponse(trip);
    }

    @Transactional
    public TripResponse cancelTrip(Long userId, Long id) {
        Trip trip = findOwnedTrip(userId, id);
        trip.cancel();
        return buildResponse(trip);
    }

    @Transactional
    public TripResponse addCityToTrip(Long userId, Long tripId, AddCityToTripRequest request) {
        Trip trip = findOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Can only add cities to PLANNED trips");
        }
        if (tripCityRepository.existsByTripIdAndCityId(tripId, request.getCityId())) {
            throw new IllegalArgumentException("City already added to this trip");
        }

        City city = cityService.findCityOrThrow(request.getCityId());
        TripCity tripCity = TripCity.builder()
                .trip(trip)
                .city(city)
                .visitOrder(request.getVisitOrder())
                .arrivalDate(request.getArrivalDate())
                .departureDate(request.getDepartureDate())
                .build();

        trip.getTripCities().add(tripCity);
        Trip saved = tripRepository.save(trip);
        return buildResponse(saved);
    }

    @Transactional
    public TripResponse removeCityFromTrip(Long userId, Long tripId, Long cityId) {
        Trip trip = findOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Can only remove cities from PLANNED trips");
        }
        trip.getTripCities().removeIf(tc -> tc.getCity().getId().equals(cityId));
        Trip saved = tripRepository.save(trip);
        return buildResponse(saved);
    }

    @Transactional
    public void deleteTrip(Long userId, Long id) {
        Trip trip = findOwnedTrip(userId, id);
        tripRepository.delete(trip);
    }

    @Transactional
    public TripResponse reorderCities(Long userId, Long tripId, List<Long> cityIds) {
        Trip trip = findOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Can only reorder cities in PLANNED trips");
        }

        for (TripCity tc : trip.getTripCities()) {
            int index = cityIds.indexOf(tc.getCity().getId());
            if (index == -1) {
                throw new IllegalArgumentException("City ID " + tc.getCity().getId() + " not found in reorder list");
            }
            tc.setVisitOrder(index + 1);
        }

        return buildResponse(trip);
    }

    @Transactional
    public TripResponse optimizeRoute(Long userId, Long tripId) {
        Trip trip = findOwnedTrip(userId, tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Can only optimize PLANNED trips");
        }

        List<TripCity> cities = new ArrayList<>(trip.getTripCities());
        if (cities.size() < 3) return buildResponse(trip);

        cities.sort(Comparator.comparingInt(TripCity::getVisitOrder));

        List<TripCity> optimized = new ArrayList<>();
        optimized.add(cities.remove(0));

        while (!cities.isEmpty()) {
            TripCity last = optimized.get(optimized.size() - 1);
            TripCity nearest = null;
            double minDist = Double.MAX_VALUE;

            for (TripCity candidate : cities) {
                double dist = RecommendationService.haversineDistance(
                        last.getCity().getLatitude(), last.getCity().getLongitude(),
                        candidate.getCity().getLatitude(), candidate.getCity().getLongitude());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = candidate;
                }
            }

            cities.remove(nearest);
            optimized.add(nearest);
        }

        for (int i = 0; i < optimized.size(); i++) {
            optimized.get(i).setVisitOrder(i + 1);
        }

        return buildResponse(trip);
    }

    public Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found with id: " + id));
    }

    private Trip findOwnedTrip(Long userId, Long tripId) {
        Trip trip = findTripOrThrow(tripId);
        if (!trip.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You don't have access to this trip");
        }
        return trip;
    }

    private TripResponse buildResponse(Trip trip) {
        TripResponse response = tripMapper.toResponse(trip);
        enrichWithRouteData(response);
        return response;
    }

    private void enrichWithRouteData(TripResponse response) {
        List<TripCityResponse> cities = response.getCities();
        if (cities == null || cities.isEmpty()) return;

        cities.sort(Comparator.comparingInt(TripCityResponse::getVisitOrder));

        if (cities.size() < 2) return;

        double totalDistance = 0;

        for (int i = 1; i < cities.size(); i++) {
            TripCityResponse prev = cities.get(i - 1);
            TripCityResponse curr = cities.get(i);

            if (prev.getLatitude() != null && curr.getLatitude() != null) {
                double dist = RecommendationService.haversineDistance(
                        prev.getLatitude(), prev.getLongitude(),
                        curr.getLatitude(), curr.getLongitude());
                curr.setDistanceFromPrevious(Math.round(dist * 10.0) / 10.0);
                totalDistance += dist;
            }
        }

        for (int i = 1; i < cities.size() - 1; i++) {
            TripCityResponse prev = cities.get(i - 1);
            TripCityResponse curr = cities.get(i);
            TripCityResponse next = cities.get(i + 1);

            if (prev.getLatitude() != null && curr.getLatitude() != null && next.getLatitude() != null) {
                double detour = RecommendationService.haversineDistance(
                        prev.getLatitude(), prev.getLongitude(),
                        curr.getLatitude(), curr.getLongitude())
                        + RecommendationService.haversineDistance(
                        curr.getLatitude(), curr.getLongitude(),
                        next.getLatitude(), next.getLongitude());
                double direct = RecommendationService.haversineDistance(
                        prev.getLatitude(), prev.getLongitude(),
                        next.getLatitude(), next.getLongitude());

                curr.setSuboptimalOrder(direct > 0 && detour > direct * 1.8);
            }
        }

        response.setTotalDistance(totalDistance > 0 ? Math.round(totalDistance * 10.0) / 10.0 : null);
    }
}