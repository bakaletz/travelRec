package com.travelRec.service;

import com.travelRec.dto.trip.AddCityToTripRequest;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.*;
import com.travelRec.entity.enums.*;
import com.travelRec.mapper.TripMapper;
import com.travelRec.repository.TripCityRepository;
import com.travelRec.repository.TripRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripCityRepository tripCityRepository;

    @Mock
    private UserService userService;

    @Mock
    private CityService cityService;

    @Spy
    private TripMapper tripMapper = new TripMapper();

    @InjectMocks
    private TripService tripService;

    private User user;
    private Trip trip;
    private City city;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("anna@mail.com").firstName("Anna").lastName("S").role(Role.USER).build();

        trip = Trip.builder()
                .id(1L).user(user).name("Summer Europe")
                .status(TripStatus.PLANNED)
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 24))
                .tripCities(new ArrayList<>())
                .ratings(new ArrayList<>())
                .build();

        Country country = Country.builder().id(1L).name("Hungary").code("HU").continent(Continent.EUROPE).build();
        city = City.builder()
                .id(1L).country(country).name("Budapest")
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .build();
    }

    @Nested
    @DisplayName("createTrip()")
    class CreateTrip {

        @Test
        @DisplayName("should create trip with PLANNED status")
        void shouldCreate() {
            TripRequest request = TripRequest.builder()
                    .name("Summer Trip")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 15))
                    .build();

            when(userService.findUserOrThrow(1L)).thenReturn(user);
            when(tripRepository.save(any(Trip.class))).thenAnswer(inv -> {
                Trip t = inv.getArgument(0);
                t.setId(2L);
                return t;
            });

            TripResponse response = tripService.createTrip(1L, request);

            assertEquals("Summer Trip", response.getName());
            assertEquals(TripStatus.PLANNED, response.getStatus());
        }
    }

    @Nested
    @DisplayName("completeTrip()")
    class CompleteTrip {

        @Test
        @DisplayName("should change status to COMPLETED")
        void shouldComplete() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.completeTrip(1L);

            assertEquals(TripStatus.COMPLETED, response.getStatus());
        }

        @Test
        @DisplayName("should throw for non-PLANNED trip")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.completeTrip(1L));
        }
    }

    @Nested
    @DisplayName("cancelTrip()")
    class CancelTrip {

        @Test
        @DisplayName("should change status to CANCELLED")
        void shouldCancel() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.cancelTrip(1L);

            assertEquals(TripStatus.CANCELLED, response.getStatus());
        }

        @Test
        @DisplayName("should throw for COMPLETED trip")
        void shouldThrowForCompleted() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.cancelTrip(1L));
        }
    }

    @Nested
    @DisplayName("updateTrip()")
    class UpdateTrip {

        @Test
        @DisplayName("should update PLANNED trip using dirty checking (no explicit save)")
        void shouldUpdate() {
            TripRequest request = TripRequest.builder()
                    .name("Updated")
                    .startDate(LocalDate.of(2026, 8, 1))
                    .build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.updateTrip(1L, request);

            assertEquals("Updated", response.getName());
            assertEquals(LocalDate.of(2026, 8, 1), response.getStartDate());

            verify(tripRepository, never()).save(any(Trip.class));
        }

        @Test
        @DisplayName("should throw when updating non-PLANNED trip")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            TripRequest request = TripRequest.builder().name("Updated").build();
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.updateTrip(1L, request));
        }
    }

    @Nested
    @DisplayName("addCityToTrip()")
    class AddCityToTrip {

        @Test
        @DisplayName("should add city to planned trip")
        void shouldAddCity() {
            AddCityToTripRequest request = AddCityToTripRequest.builder()
                    .cityId(1L).visitOrder(1)
                    .arrivalDate(LocalDate.of(2026, 7, 10))
                    .departureDate(LocalDate.of(2026, 7, 15))
                    .build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.existsByTripIdAndCityId(1L, 1L)).thenReturn(false);
            when(cityService.findCityOrThrow(1L)).thenReturn(city);
            when(tripRepository.save(any(Trip.class))).thenReturn(trip);

            TripResponse response = tripService.addCityToTrip(1L, request);

            assertEquals(1, trip.getTripCities().size());
        }

        @Test
        @DisplayName("should throw when city already in trip")
        void shouldThrowWhenDuplicate() {
            AddCityToTripRequest request = AddCityToTripRequest.builder().cityId(1L).visitOrder(1).build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.existsByTripIdAndCityId(1L, 1L)).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> tripService.addCityToTrip(1L, request));
        }

        @Test
        @DisplayName("should throw when trip is not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            AddCityToTripRequest request = AddCityToTripRequest.builder().cityId(1L).visitOrder(1).build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.addCityToTrip(1L, request));
        }
    }

    @Nested
    @DisplayName("removeCityFromTrip()")
    class RemoveCityFromTrip {

        @Test
        @DisplayName("should remove city from planned trip")
        void shouldRemove() {
            TripCity tripCity = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tripCity);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripRepository.save(any(Trip.class))).thenReturn(trip);

            tripService.removeCityFromTrip(1L, 1L);

            assertTrue(trip.getTripCities().isEmpty());
        }

        @Test
        @DisplayName("should throw when trip is not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.RATED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.removeCityFromTrip(1L, 1L));
        }
    }

    @Nested
    @DisplayName("getTripById()")
    class GetTripById {

        @Test
        @DisplayName("should throw when trip not found")
        void shouldThrowWhenNotFound() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tripService.getTripById(99L));
        }
    }
}
