package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.trip.AddCityToTripRequest;
import com.travelRec.dto.trip.TripRequest;
import com.travelRec.dto.trip.TripResponse;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.service.TripService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import com.travelRec.repository.UserRepository;
import com.travelRec.security.JwtUtil;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private TripService tripService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private RequestPostProcessor authUser;

    @BeforeEach
    void setUp() {
        authUser = TestAuth.user();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TripResponse buildTrip() {
        return TripResponse.builder()
                .id(1L).name("Summer Trip")
                .status(TripStatus.PLANNED)
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 24))
                .cities(List.of()).build();
    }

    @Nested
    @DisplayName("GET /api/trips")
    class GetTrips {

        @Test
        @DisplayName("should return all user trips when no status")
        void shouldReturnAll() throws Exception {
            when(tripService.getUserTrips(1L)).thenReturn(List.of(buildTrip()));

            mockMvc.perform(get("/api/trips").with(authUser))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Summer Trip"));

            verify(tripService).getUserTrips(1L);
        }

        @Test
        @DisplayName("should filter by status when provided")
        void shouldFilterByStatus() throws Exception {
            when(tripService.getUserTripsByStatus(1L, TripStatus.COMPLETED))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/trips").with(authUser).param("status", "COMPLETED"))
                    .andExpect(status().isOk());

            verify(tripService).getUserTripsByStatus(1L, TripStatus.COMPLETED);
            verify(tripService, never()).getUserTrips(any());
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{id}")
    class GetById {

        @Test
        @DisplayName("should return trip")
        void shouldReturn() throws Exception {
            when(tripService.getTripById(1L, 1L)).thenReturn(buildTrip());

            mockMvc.perform(get("/api/trips/1").with(authUser))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/trips")
    class CreateTrip {

        @Test
        @DisplayName("should create trip and return 201")
        void shouldCreate() throws Exception {
            TripRequest request = TripRequest.builder()
                    .name("Summer Trip")
                    .startDate(LocalDate.of(2026, 7, 10))
                    .endDate(LocalDate.of(2026, 7, 24))
                    .build();

            when(tripService.createTrip(eq(1L), any(TripRequest.class))).thenReturn(buildTrip());

            mockMvc.perform(post("/api/trips")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            TripRequest request = TripRequest.builder().name("").build();

            mockMvc.perform(post("/api/trips")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/trips/{id}/cities")
    class AddCity {

        @Test
        @DisplayName("should add city to trip")
        void shouldAdd() throws Exception {
            AddCityToTripRequest request = AddCityToTripRequest.builder()
                    .cityId(1L).visitOrder(1)
                    .arrivalDate(LocalDate.of(2026, 7, 10))
                    .departureDate(LocalDate.of(2026, 7, 15))
                    .build();

            when(tripService.addCityToTrip(eq(1L), eq(1L), any(AddCityToTripRequest.class)))
                    .thenReturn(buildTrip());

            mockMvc.perform(post("/api/trips/1/cities")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 when cityId is null")
        void shouldRejectNullCityId() throws Exception {
            AddCityToTripRequest request = AddCityToTripRequest.builder()
                    .visitOrder(1).build();

            mockMvc.perform(post("/api/trips/1/cities")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/trips/{id}")
    class Update {

        @Test
        @DisplayName("should update trip")
        void shouldUpdate() throws Exception {
            TripRequest request = TripRequest.builder().name("Updated").build();

            when(tripService.updateTrip(eq(1L), eq(1L), any(TripRequest.class)))
                    .thenReturn(buildTrip());

            mockMvc.perform(put("/api/trips/1")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH endpoints")
    class PatchEndpoints {

        @Test
        @DisplayName("PATCH /complete should call completeTrip")
        void shouldComplete() throws Exception {
            when(tripService.completeTrip(1L, 1L)).thenReturn(buildTrip());

            mockMvc.perform(patch("/api/trips/1/complete").with(authUser))
                    .andExpect(status().isOk());

            verify(tripService).completeTrip(1L, 1L);
        }

        @Test
        @DisplayName("PATCH /cancel should call cancelTrip")
        void shouldCancel() throws Exception {
            when(tripService.cancelTrip(1L, 1L)).thenReturn(buildTrip());

            mockMvc.perform(patch("/api/trips/1/cancel").with(authUser))
                    .andExpect(status().isOk());

            verify(tripService).cancelTrip(1L, 1L);
        }

        @Test
        @DisplayName("PATCH /reorder should call reorderCities with city ids")
        void shouldReorder() throws Exception {
            when(tripService.reorderCities(eq(1L), eq(1L), any())).thenReturn(buildTrip());

            mockMvc.perform(patch("/api/trips/1/reorder")
                            .with(authUser)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[3, 1, 2]"))
                    .andExpect(status().isOk());

            verify(tripService).reorderCities(eq(1L), eq(1L), eq(List.of(3L, 1L, 2L)));
        }

        @Test
        @DisplayName("PATCH /optimize should call optimizeRoute")
        void shouldOptimize() throws Exception {
            when(tripService.optimizeRoute(1L, 1L)).thenReturn(buildTrip());

            mockMvc.perform(patch("/api/trips/1/optimize").with(authUser))
                    .andExpect(status().isOk());

            verify(tripService).optimizeRoute(1L, 1L);
        }
    }

    @Nested
    @DisplayName("DELETE endpoints")
    class DeleteEndpoints {

        @Test
        @DisplayName("DELETE /{id}/cities/{cityId} should call removeCityFromTrip")
        void shouldRemoveCity() throws Exception {
            when(tripService.removeCityFromTrip(1L, 1L, 5L)).thenReturn(buildTrip());

            mockMvc.perform(delete("/api/trips/1/cities/5").with(authUser))
                    .andExpect(status().isOk());

            verify(tripService).removeCityFromTrip(1L, 1L, 5L);
        }

        @Test
        @DisplayName("DELETE /{id} should return 204")
        void shouldDelete() throws Exception {
            mockMvc.perform(delete("/api/trips/1").with(authUser))
                    .andExpect(status().isNoContent());

            verify(tripService).deleteTrip(1L, 1L);
        }
    }
}