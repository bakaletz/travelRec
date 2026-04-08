package com.travelRec.mapper;

import com.travelRec.dto.trip.TripCityResponse;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.Trip;
import com.travelRec.entity.TripCity;
import com.travelRec.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TripMapper {

    public Trip toEntity(TripRequest request, User user) {
        return Trip.builder()
                .user(user)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
    }

    public TripResponse toResponse(Trip trip) {
        List<TripCityResponse> cities = trip.getTripCities().stream()
                .map(this::toTripCityResponse)
                .collect(Collectors.toList());

        return TripResponse.builder()
                .id(trip.getId())
                .name(trip.getName())
                .status(trip.getStatus())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .createdAt(trip.getCreatedAt())
                .cities(cities)
                .build();
    }

    public TripCityResponse toTripCityResponse(TripCity tripCity) {
        return TripCityResponse.builder()
                .id(tripCity.getId())
                .cityId(tripCity.getCity().getId())
                .cityName(tripCity.getCity().getName())
                .countryName(tripCity.getCity().getCountry().getName())
                .imageUrl(tripCity.getCity().getImageUrl())
                .visitOrder(tripCity.getVisitOrder())
                .arrivalDate(tripCity.getArrivalDate())
                .departureDate(tripCity.getDepartureDate())
                .build();
    }

    public void updateEntity(Trip trip, TripRequest request) {
        trip.setName(request.getName());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
    }
}
