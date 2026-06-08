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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
    private User otherUser;
    private Trip trip;
    private City city;
    private Country country;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("anna@mail.com").firstName("Anna").lastName("S").role(Role.USER).build();
        otherUser = User.builder().id(2L).email("bob@mail.com").firstName("Bob").lastName("X").role(Role.USER).build();

        country = Country.builder().id(1L).name("Hungary").code("HU").continent(Continent.EUROPE).build();

        city = City.builder()
                .id(1L).country(country).name("Budapest")
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .latitude(47.4979).longitude(19.0402)
                .build();

        trip = Trip.builder()
                .id(1L).user(user).name("Summer Europe")
                .status(TripStatus.PLANNED)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(3))
                .tripCities(new ArrayList<>())
                .ratings(new ArrayList<>())
                .build();
    }

    private City buildCity(Long id, String name, double lat, double lng) {
        return City.builder()
                .id(id).country(country).name(name)
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .latitude(lat).longitude(lng)
                .build();
    }

    @Nested
    @DisplayName("getUserTrips()")
    class GetUserTrips {

        @Test
        @DisplayName("should return trips for user")
        void shouldReturnTrips() {
            when(tripRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(trip));

            List<TripResponse> result = tripService.getUserTrips(1L);

            assertEquals(1, result.size());
            assertEquals("Summer Europe", result.get(0).getName());
        }

        @Test
        @DisplayName("should return empty list when no trips")
        void shouldReturnEmpty() {
            when(tripRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            assertTrue(tripService.getUserTrips(1L).isEmpty());
        }
    }

    @Nested
    @DisplayName("getUserTripsByStatus()")
    class GetUserTripsByStatus {

        @Test
        @DisplayName("should filter trips by status")
        void shouldFilter() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, TripStatus.COMPLETED))
                    .thenReturn(List.of(trip));

            List<TripResponse> result = tripService.getUserTripsByStatus(1L, TripStatus.COMPLETED);

            assertEquals(1, result.size());
            assertEquals(TripStatus.COMPLETED, result.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("getTripById()")
    class GetTripById {

        @Test
        @DisplayName("should return owned trip")
        void shouldReturnOwned() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.getTripById(1L, 1L);

            assertEquals("Summer Europe", response.getName());
        }

        @Test
        @DisplayName("should throw AccessDenied when trip belongs to another user")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.getTripById(2L, 1L));
        }

        @Test
        @DisplayName("should throw when trip not found")
        void shouldThrowNotFound() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tripService.getTripById(1L, 99L));
        }
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
    @DisplayName("updateTrip()")
    class UpdateTrip {

        @Test
        @DisplayName("should update PLANNED trip via dirty checking")
        void shouldUpdate() {
            TripRequest request = TripRequest.builder()
                    .name("Updated")
                    .startDate(LocalDate.of(2026, 8, 1))
                    .endDate(LocalDate.of(2026, 8, 15))
                    .build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.updateTrip(1L, 1L, request);

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

            assertThrows(IllegalStateException.class, () -> tripService.updateTrip(1L, 1L, request));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            TripRequest request = TripRequest.builder().name("Updated").build();
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.updateTrip(2L, 1L, request));
        }
    }

    @Nested
    @DisplayName("completeTrip()")
    class CompleteTrip {

        @Test
        @DisplayName("should change status to COMPLETED")
        void shouldComplete() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.completeTrip(1L, 1L);

            assertEquals(TripStatus.COMPLETED, response.getStatus());
        }

        @Test
        @DisplayName("should throw for non-PLANNED trip")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.completeTrip(1L, 1L));
        }

        @Test
        @DisplayName("should increment popularity for cities not previously completed")
        void shouldIncrementPopularity() {
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(0L);

            tripService.completeTrip(1L, 1L);

            verify(cityService).incrementPopularity(1L);
        }

        @Test
        @DisplayName("should NOT increment popularity if user already completed this city")
        void shouldNotIncrementPopularityIfRepeat() {
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(1L);

            tripService.completeTrip(1L, 1L);

            verify(cityService, never()).incrementPopularity(anyLong());
        }

        @Test
        @DisplayName("should handle multiple cities in trip")
        void shouldHandleMultipleCities() {
            City city2 = buildCity(2L, "Vienna", 48.2082, 16.3738);
            TripCity tc1 = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            TripCity tc2 = TripCity.builder().id(2L).trip(trip).city(city2).visitOrder(2).build();
            trip.getTripCities().addAll(List.of(tc1, tc2));

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(0L);
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 2L, 1L)).thenReturn(0L);

            tripService.completeTrip(1L, 1L);

            verify(cityService).incrementPopularity(1L);
            verify(cityService).incrementPopularity(2L);
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.completeTrip(2L, 1L));
        }
    }

    @Nested
    @DisplayName("cancelTrip()")
    class CancelTrip {

        @Test
        @DisplayName("should change status to CANCELLED")
        void shouldCancel() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            TripResponse response = tripService.cancelTrip(1L, 1L);

            assertEquals(TripStatus.CANCELLED, response.getStatus());
        }

        @Test
        @DisplayName("should throw for COMPLETED trip")
        void shouldThrowForCompleted() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.cancelTrip(1L, 1L));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.cancelTrip(2L, 1L));
        }
    }

    @Nested
    @DisplayName("addCityToTrip()")
    class AddCityToTripTests {

        @Test
        @DisplayName("should add city to PLANNED trip")
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

            tripService.addCityToTrip(1L, 1L, request);

            assertEquals(1, trip.getTripCities().size());
            assertEquals(city, trip.getTripCities().get(0).getCity());
        }

        @Test
        @DisplayName("should throw when city already in trip")
        void shouldThrowWhenDuplicate() {
            AddCityToTripRequest request = AddCityToTripRequest.builder().cityId(1L).visitOrder(1).build();

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.existsByTripIdAndCityId(1L, 1L)).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> tripService.addCityToTrip(1L, 1L, request));
        }

        @Test
        @DisplayName("should throw when trip not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            AddCityToTripRequest request = AddCityToTripRequest.builder().cityId(1L).visitOrder(1).build();
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.addCityToTrip(1L, 1L, request));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            AddCityToTripRequest request = AddCityToTripRequest.builder().cityId(1L).visitOrder(1).build();
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.addCityToTrip(2L, 1L, request));
        }
    }

    @Nested
    @DisplayName("removeCityFromTrip()")
    class RemoveCityFromTrip {

        @Test
        @DisplayName("should remove city from PLANNED trip")
        void shouldRemove() {
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripRepository.save(any(Trip.class))).thenReturn(trip);

            tripService.removeCityFromTrip(1L, 1L, 1L);

            assertTrue(trip.getTripCities().isEmpty());
        }

        @Test
        @DisplayName("should throw when trip not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.RATED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.removeCityFromTrip(1L, 1L, 1L));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.removeCityFromTrip(2L, 1L, 1L));
        }
    }

    @Nested
    @DisplayName("deleteTrip()")
    class DeleteTrip {

        @Test
        @DisplayName("should delete PLANNED trip without affecting popularity")
        void shouldDeletePlanned() {
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.deleteTrip(1L, 1L);

            verify(tripRepository).delete(trip);
            verify(cityService, never()).decrementPopularity(anyLong());
        }

        @Test
        @DisplayName("should delete CANCELLED trip without affecting popularity")
        void shouldDeleteCancelled() {
            trip.setStatus(TripStatus.CANCELLED);
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.deleteTrip(1L, 1L);

            verify(cityService, never()).decrementPopularity(anyLong());
        }

        @Test
        @DisplayName("should decrement popularity when deleting COMPLETED trip with no other completion")
        void shouldDecrementForCompleted() {
            trip.setStatus(TripStatus.COMPLETED);
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(0L);

            tripService.deleteTrip(1L, 1L);

            verify(cityService).decrementPopularity(1L);
        }

        @Test
        @DisplayName("should decrement popularity when deleting RATED trip")
        void shouldDecrementForRated() {
            trip.setStatus(TripStatus.RATED);
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(0L);

            tripService.deleteTrip(1L, 1L);

            verify(cityService).decrementPopularity(1L);
        }

        @Test
        @DisplayName("should NOT decrement when user has another completed trip to same city")
        void shouldNotDecrementWhenOtherTripExists() {
            trip.setStatus(TripStatus.COMPLETED);
            TripCity tc = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
            when(tripCityRepository.countOtherCompletedTripsWithCity(1L, 1L, 1L)).thenReturn(1L);

            tripService.deleteTrip(1L, 1L);

            verify(cityService, never()).decrementPopularity(anyLong());
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.deleteTrip(2L, 1L));
        }
    }

    @Nested
    @DisplayName("reorderCities()")
    class ReorderCities {

        @Test
        @DisplayName("should reorder cities according to provided list")
        void shouldReorder() {
            City city2 = buildCity(2L, "Vienna", 48.2082, 16.3738);
            City city3 = buildCity(3L, "Prague", 50.0755, 14.4378);

            TripCity tc1 = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            TripCity tc2 = TripCity.builder().id(2L).trip(trip).city(city2).visitOrder(2).build();
            TripCity tc3 = TripCity.builder().id(3L).trip(trip).city(city3).visitOrder(3).build();
            trip.getTripCities().addAll(List.of(tc1, tc2, tc3));

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.reorderCities(1L, 1L, List.of(3L, 1L, 2L));

            assertEquals(2, tc1.getVisitOrder());
            assertEquals(3, tc2.getVisitOrder());
            assertEquals(1, tc3.getVisitOrder());
        }

        @Test
        @DisplayName("should throw when reorder list contains unknown cityId")
        void shouldThrowForUnknownCity() {
            TripCity tc1 = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            trip.getTripCities().add(tc1);

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalArgumentException.class,
                    () -> tripService.reorderCities(1L, 1L, List.of(99L)));
        }

        @Test
        @DisplayName("should throw when trip not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class,
                    () -> tripService.reorderCities(1L, 1L, List.of(1L)));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class,
                    () -> tripService.reorderCities(2L, 1L, List.of(1L)));
        }
    }

    @Nested
    @DisplayName("optimizeRoute()")
    class OptimizeRoute {

        @Test
        @DisplayName("should not change order when fewer than 3 cities")
        void shouldNotChangeForLessThanThree() {
            City city2 = buildCity(2L, "Vienna", 48.2082, 16.3738);
            TripCity tc1 = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();
            TripCity tc2 = TripCity.builder().id(2L).trip(trip).city(city2).visitOrder(2).build();
            trip.getTripCities().addAll(List.of(tc1, tc2));

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.optimizeRoute(1L, 1L);

            assertEquals(1, tc1.getVisitOrder());
            assertEquals(2, tc2.getVisitOrder());
        }

        @Test
        @DisplayName("should reorder cities by nearest-neighbor heuristic starting from first")
        void shouldOptimizeByNearestNeighbor() {
            // Budapest (47.5, 19.0) -> Paris (48.85, 2.35) -> Vienna (48.2, 16.4) -> Munich (48.13, 11.58)
            // After optimization starting from Budapest, order should pick nearest: Vienna, Munich, Paris
            City vienna = buildCity(2L, "Vienna", 48.2082, 16.3738);
            City munich = buildCity(3L, "Munich", 48.1351, 11.5820);
            City paris = buildCity(4L, "Paris", 48.8566, 2.3522);

            TripCity tc1 = TripCity.builder().id(1L).trip(trip).city(city).visitOrder(1).build();   // Budapest
            TripCity tc2 = TripCity.builder().id(2L).trip(trip).city(paris).visitOrder(2).build();
            TripCity tc3 = TripCity.builder().id(3L).trip(trip).city(vienna).visitOrder(3).build();
            TripCity tc4 = TripCity.builder().id(4L).trip(trip).city(munich).visitOrder(4).build();
            trip.getTripCities().addAll(List.of(tc1, tc2, tc3, tc4));

            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            tripService.optimizeRoute(1L, 1L);

            // Budapest stays first
            assertEquals(1, tc1.getVisitOrder());
            // Vienna is closest to Budapest -> 2nd
            assertEquals(2, tc3.getVisitOrder());
            // Munich is closest to Vienna -> 3rd
            assertEquals(3, tc4.getVisitOrder());
            // Paris is last
            assertEquals(4, tc2.getVisitOrder());
        }

        @Test
        @DisplayName("should throw when trip not PLANNED")
        void shouldThrowForNonPlanned() {
            trip.setStatus(TripStatus.COMPLETED);
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(IllegalStateException.class, () -> tripService.optimizeRoute(1L, 1L));
        }

        @Test
        @DisplayName("should throw AccessDenied when not owner")
        void shouldThrowAccessDenied() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            assertThrows(AccessDeniedException.class, () -> tripService.optimizeRoute(2L, 1L));
        }
    }

    @Nested
    @DisplayName("findTripOrThrow()")
    class FindTripOrThrow {

        @Test
        @DisplayName("should return trip when found")
        void shouldReturnTrip() {
            when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

            Trip found = tripService.findTripOrThrow(1L);

            assertEquals(trip, found);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(tripRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> tripService.findTripOrThrow(99L));
        }
    }
}