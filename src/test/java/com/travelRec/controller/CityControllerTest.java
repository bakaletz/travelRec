package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.city.CityRequest;
import com.travelRec.dto.city.CityResponse;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import com.travelRec.service.CityService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@AutoConfigureMockMvc(addFilters = false)
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private CityService cityService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private CityResponse buildResponse() {
        return CityResponse.builder()
                .id(1L).name("Budapest").region("Central Hungary")
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .latitude(47.4979).longitude(19.0402)
                .countryId(1L).countryName("Hungary").countryCode("HU")
                .build();
    }

    private CityRequest buildValidRequest() {
        return CityRequest.builder()
                .countryId(1L).name("Budapest")
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.CONTINENTAL)
                .latitude(47.4979).longitude(19.0402)
                .baseCostLevel(0.35f).baseSafetyScore(0.7f)
                .baseCultureScore(0.85f).baseFoodScore(0.8f)
                .baseNightlifeScore(0.9f).baseNatureScore(0.6f)
                .baseBeachScore(0.15f).baseArchitectureScore(0.9f)
                .baseShoppingScore(0.5f)
                .publicTransportScore(0.8f).walkabilityScore(0.7f)
                .build();
    }

    @Nested
    @DisplayName("GET /api/cities")
    class GetCities {

        @Test
        @DisplayName("should return all cities when no filter")
        void shouldReturnAll() throws Exception {
            when(cityService.getAllCities()).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name").value("Budapest"));

            verify(cityService).getAllCities();
        }

        @Test
        @DisplayName("should filter by search query")
        void shouldSearch() throws Exception {
            when(cityService.searchCities("buda")).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities").param("search", "buda"))
                    .andExpect(status().isOk());

            verify(cityService).searchCities("buda");
        }

        @Test
        @DisplayName("should filter by continent")
        void shouldFilterByContinent() throws Exception {
            when(cityService.getCitiesByContinent(Continent.EUROPE))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities").param("continent", "EUROPE"))
                    .andExpect(status().isOk());

            verify(cityService).getCitiesByContinent(Continent.EUROPE);
        }

        @Test
        @DisplayName("should filter by city type")
        void shouldFilterByCityType() throws Exception {
            when(cityService.getCitiesByCityType(CityType.LARGE_CITY))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities").param("cityType", "LARGE_CITY"))
                    .andExpect(status().isOk());

            verify(cityService).getCitiesByCityType(CityType.LARGE_CITY);
        }

        @Test
        @DisplayName("should filter by climate type")
        void shouldFilterByClimate() throws Exception {
            when(cityService.getCitiesByClimateType(ClimateType.CONTINENTAL))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities").param("climateType", "CONTINENTAL"))
                    .andExpect(status().isOk());

            verify(cityService).getCitiesByClimateType(ClimateType.CONTINENTAL);
        }

        @Test
        @DisplayName("should filter by country id")
        void shouldFilterByCountry() throws Exception {
            when(cityService.getCitiesByCountry(1L)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities").param("countryId", "1"))
                    .andExpect(status().isOk());

            verify(cityService).getCitiesByCountry(1L);
        }

        @Test
        @DisplayName("search has highest priority")
        void searchHasPriority() throws Exception {
            when(cityService.searchCities("buda")).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities")
                            .param("search", "buda")
                            .param("continent", "EUROPE")
                            .param("countryId", "1"))
                    .andExpect(status().isOk());

            verify(cityService).searchCities("buda");
            verify(cityService, never()).getCitiesByContinent(any());
            verify(cityService, never()).getCitiesByCountry(any());
        }
    }

    @Nested
    @DisplayName("GET /api/cities/{id}")
    class GetById {

        @Test
        @DisplayName("should return city")
        void shouldReturn() throws Exception {
            when(cityService.getCityById(1L)).thenReturn(buildResponse());

            mockMvc.perform(get("/api/cities/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Budapest"));
        }
    }

    @Nested
    @DisplayName("GET /api/cities/{id}/nearby")
    class Nearby {

        @Test
        @DisplayName("should use default radius when not provided")
        void shouldUseDefault() throws Exception {
            when(cityService.getNearbyCities(1L, 300.0)).thenReturn(List.of());

            mockMvc.perform(get("/api/cities/1/nearby"))
                    .andExpect(status().isOk());

            verify(cityService).getNearbyCities(1L, 300.0);
        }

        @Test
        @DisplayName("should use provided radius")
        void shouldUseProvidedRadius() throws Exception {
            when(cityService.getNearbyCities(1L, 500.0)).thenReturn(List.of());

            mockMvc.perform(get("/api/cities/1/nearby").param("radiusKm", "500"))
                    .andExpect(status().isOk());

            verify(cityService).getNearbyCities(1L, 500.0);
        }
    }

    @Nested
    @DisplayName("GET /api/cities/{id}/same-country")
    class SameCountry {

        @Test
        @DisplayName("should return cities in same country")
        void shouldReturn() throws Exception {
            when(cityService.getCitiesInSameCountry(1L)).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/cities/1/same-country"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/cities")
    class Create {

        @Test
        @DisplayName("should return 201 CREATED on valid request")
        void shouldCreate() throws Exception {
            when(cityService.createCity(any(CityRequest.class))).thenReturn(buildResponse());

            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldRejectBlankName() throws Exception {
            CityRequest request = buildValidRequest();
            request.setName("");

            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when latitude out of range")
        void shouldRejectInvalidLatitude() throws Exception {
            CityRequest request = buildValidRequest();
            request.setLatitude(99.0);

            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when score out of [0, 1]")
        void shouldRejectInvalidScore() throws Exception {
            CityRequest request = buildValidRequest();
            request.setBaseCultureScore(1.5f);

            mockMvc.perform(post("/api/cities")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/cities/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 OK on valid update")
        void shouldUpdate() throws Exception {
            when(cityService.updateCity(eq(1L), any(CityRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(put("/api/cities/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildValidRequest())))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/cities/{id}/recalculate")
    class Recalculate {

        @Test
        @DisplayName("should return 200 OK")
        void shouldRecalculate() throws Exception {
            mockMvc.perform(patch("/api/cities/1/recalculate"))
                    .andExpect(status().isOk());

            verify(cityService).recalculateScores(1L);
        }
    }

    @Nested
    @DisplayName("DELETE /api/cities/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 No Content")
        void shouldDelete() throws Exception {
            mockMvc.perform(delete("/api/cities/1"))
                    .andExpect(status().isNoContent());

            verify(cityService).deleteCity(1L);
        }
    }
}