package com.travelRec.mapper;

import com.travelRec.dto.trip.TripCityResponse;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TripMapperTest {

    private TripMapper mapper;
    private User user;

    @BeforeEach
    void setUp() {
        mapper = new TripMapper();
        user = User.builder()
                .id(1L)
                .email("anna@mail.com")
                .firstName("Anna")
                .lastName("Shevchenko")
                .role(Role.USER)
                .build();
    }

    private TripRequest buildRequest() {
        return TripRequest.builder()
                .name("Summer Europe 2026")
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 24))
                .build();
    }

    private Trip buildTrip() {
        Country country = Country.builder()
                .id(1L)
                .name("Hungary")
                .code("HU")
                .continent(Continent.EUROPE)
                .build();

        City city = City.builder()
                .id(1L)
                .name("Budapest")
                .country(country)
                .imageUrl("https://example.com/budapest.jpg")
                .build();

        TripCity tripCity = TripCity.builder()
                .id(1L)
                .city(city)
                .visitOrder(1)
                .arrivalDate(LocalDate.of(2026, 7, 10))
                .departureDate(LocalDate.of(2026, 7, 15))
                .build();

        Trip trip = Trip.builder()
                .id(1L)
                .user(user)
                .name("Summer Europe 2026")
                .status(TripStatus.PLANNED)
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 24))
                .tripCities(new ArrayList<>(List.of(tripCity)))
                .ratings(new ArrayList<>())
                .build();

        tripCity.setTrip(trip);
        return trip;
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from request")
        void shouldMapAllFields() {
            TripRequest request = buildRequest();
            Trip trip = mapper.toEntity(request, user);

            assertEquals("Summer Europe 2026", trip.getName());
            assertEquals(LocalDate.of(2026, 7, 10), trip.getStartDate());
            assertEquals(LocalDate.of(2026, 7, 24), trip.getEndDate());
        }

        @Test
        @DisplayName("should set user reference")
        void shouldSetUser() {
            TripRequest request = buildRequest();
            Trip trip = mapper.toEntity(request, user);

            assertEquals(user, trip.getUser());
            assertEquals(1L, trip.getUser().getId());
        }

        @Test
        @DisplayName("should set default status PLANNED")
        void shouldSetDefaultStatus() {
            TripRequest request = buildRequest();
            Trip trip = mapper.toEntity(request, user);

            assertEquals(TripStatus.PLANNED, trip.getStatus());
        }

        @Test
        @DisplayName("should not set id")
        void shouldNotSetId() {
            TripRequest request = buildRequest();
            Trip trip = mapper.toEntity(request, user);

            assertNull(trip.getId());
        }

        @Test
        @DisplayName("should handle null dates")
        void shouldHandleNullDates() {
            TripRequest request = TripRequest.builder()
                    .name("Quick trip")
                    .build();

            Trip trip = mapper.toEntity(request, user);

            assertEquals("Quick trip", trip.getName());
            assertNull(trip.getStartDate());
            assertNull(trip.getEndDate());
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all fields to response")
        void shouldMapAllFields() {
            Trip trip = buildTrip();
            TripResponse response = mapper.toResponse(trip);

            assertEquals(1L, response.getId());
            assertEquals("Summer Europe 2026", response.getName());
            assertEquals(TripStatus.PLANNED, response.getStatus());
            assertEquals(LocalDate.of(2026, 7, 10), response.getStartDate());
            assertEquals(LocalDate.of(2026, 7, 24), response.getEndDate());
        }

        @Test
        @DisplayName("should map trip cities list")
        void shouldMapTripCities() {
            Trip trip = buildTrip();
            TripResponse response = mapper.toResponse(trip);

            assertNotNull(response.getCities());
            assertEquals(1, response.getCities().size());

            TripCityResponse cityResponse = response.getCities().get(0);
            assertEquals(1L, cityResponse.getCityId());
            assertEquals("Budapest", cityResponse.getCityName());
            assertEquals("Hungary", cityResponse.getCountryName());
            assertEquals(1, cityResponse.getVisitOrder());
            assertEquals(LocalDate.of(2026, 7, 10), cityResponse.getArrivalDate());
            assertEquals(LocalDate.of(2026, 7, 15), cityResponse.getDepartureDate());
        }

        @Test
        @DisplayName("should handle empty cities list")
        void shouldHandleEmptyCities() {
            Trip trip = Trip.builder()
                    .id(1L)
                    .user(user)
                    .name("Empty trip")
                    .status(TripStatus.PLANNED)
                    .tripCities(new ArrayList<>())
                    .ratings(new ArrayList<>())
                    .build();

            TripResponse response = mapper.toResponse(trip);

            assertNotNull(response.getCities());
            assertTrue(response.getCities().isEmpty());
        }

        @Test
        @DisplayName("should map city image url")
        void shouldMapCityImageUrl() {
            Trip trip = buildTrip();
            TripResponse response = mapper.toResponse(trip);

            assertEquals("https://example.com/budapest.jpg", response.getCities().get(0).getImageUrl());
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        @DisplayName("should update name and dates")
        void shouldUpdateFields() {
            Trip trip = buildTrip();
            TripRequest request = TripRequest.builder()
                    .name("Updated Trip Name")
                    .startDate(LocalDate.of(2026, 8, 1))
                    .endDate(LocalDate.of(2026, 8, 15))
                    .build();

            mapper.updateEntity(trip, request);

            assertEquals("Updated Trip Name", trip.getName());
            assertEquals(LocalDate.of(2026, 8, 1), trip.getStartDate());
            assertEquals(LocalDate.of(2026, 8, 15), trip.getEndDate());
        }

        @Test
        @DisplayName("should preserve id and status after update")
        void shouldPreserveIdAndStatus() {
            Trip trip = buildTrip();
            TripRequest request = buildRequest();
            request.setName("Updated");

            mapper.updateEntity(trip, request);

            assertEquals(1L, trip.getId());
            assertEquals(TripStatus.PLANNED, trip.getStatus());
        }

        @Test
        @DisplayName("should preserve cities after update")
        void shouldPreserveCities() {
            Trip trip = buildTrip();
            int citiesCount = trip.getTripCities().size();

            TripRequest request = buildRequest();
            request.setName("Updated");

            mapper.updateEntity(trip, request);

            assertEquals(citiesCount, trip.getTripCities().size());
        }
    }
}
