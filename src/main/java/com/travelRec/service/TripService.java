package com.travelRec.service;

import com.travelRec.dto.trip.AddCityToTripRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(tripMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<TripResponse> getUserTripsByStatus(Long userId, TripStatus status) {
        return tripRepository.findByUserIdAndStatus(userId, status).stream()
                .map(tripMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TripResponse getTripById(Long id) {
        Trip trip = findTripOrThrow(id);
        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse createTrip(Long userId, TripRequest request) {
        User user = userService.findUserOrThrow(userId);
        Trip trip = tripMapper.toEntity(request, user);
        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved);
    }

    @Transactional
    public TripResponse updateTrip(Long id, TripRequest request) {
        Trip trip = findTripOrThrow(id);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED trips can be edited");
        }

        tripMapper.updateEntity(trip, request);

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse completeTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        trip.complete();

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse cancelTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        trip.cancel();

        return tripMapper.toResponse(trip);
    }

    @Transactional
    public TripResponse addCityToTrip(Long tripId, AddCityToTripRequest request) {
        Trip trip = findTripOrThrow(tripId);
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
        return tripMapper.toResponse(saved);
    }

    @Transactional
    public TripResponse removeCityFromTrip(Long tripId, Long cityId) {
        Trip trip = findTripOrThrow(tripId);
        if (trip.getStatus() != TripStatus.PLANNED) {
            throw new IllegalStateException("Can only remove cities from PLANNED trips");
        }

        trip.getTripCities().removeIf(tc -> tc.getCity().getId().equals(cityId));
        Trip saved = tripRepository.save(trip);
        return tripMapper.toResponse(saved);
    }

    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        tripRepository.delete(trip);
    }

    public Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found with id: " + id));
    }
}
