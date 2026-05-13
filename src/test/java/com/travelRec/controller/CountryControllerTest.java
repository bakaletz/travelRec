package com.travelRec.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.enums.Continent;
import com.travelRec.service.CountryService;
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

@WebMvcTest(CountryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private CountryService countryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private CountryResponse buildResponse() {
        return CountryResponse.builder()
                .id(1L).name("Ukraine").code("UA")
                .continent(Continent.EUROPE).language("Ukrainian").currency("UAH")
                .cityCount(5).build();
    }

    @Nested
    @DisplayName("GET /api/countries")
    class GetAll {

        @Test
        @DisplayName("should return all countries")
        void shouldReturnAll() throws Exception {
            when(countryService.getAllCountries()).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].name").value("Ukraine"))
                    .andExpect(jsonPath("$[0].code").value("UA"));

            verify(countryService).getAllCountries();
        }

        @Test
        @DisplayName("should filter by continent")
        void shouldFilterByContinent() throws Exception {
            when(countryService.getCountriesByContinent(Continent.EUROPE))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/countries").param("continent", "EUROPE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].continent").value("EUROPE"));

            verify(countryService).getCountriesByContinent(Continent.EUROPE);
            verify(countryService, never()).getAllCountries();
        }

        @Test
        @DisplayName("should search by query when provided")
        void shouldSearch() throws Exception {
            when(countryService.searchCountries("ukr")).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/countries").param("search", "ukr"))
                    .andExpect(status().isOk());

            verify(countryService).searchCountries("ukr");
        }

        @Test
        @DisplayName("should prefer search over continent param")
        void shouldPreferSearch() throws Exception {
            when(countryService.searchCountries("ukr")).thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/api/countries")
                            .param("search", "ukr")
                            .param("continent", "EUROPE"))
                    .andExpect(status().isOk());

            verify(countryService).searchCountries("ukr");
            verify(countryService, never()).getCountriesByContinent(any());
        }
    }

    @Nested
    @DisplayName("GET /api/countries/{id}")
    class GetById {

        @Test
        @DisplayName("should return country by id")
        void shouldReturn() throws Exception {
            when(countryService.getCountryById(1L)).thenReturn(buildResponse());

            mockMvc.perform(get("/api/countries/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ukraine"));
        }
    }

    @Nested
    @DisplayName("POST /api/countries")
    class Create {

        @Test
        @DisplayName("should create country and return 201")
        void shouldCreate() throws Exception {
            CountryRequest request = CountryRequest.builder()
                    .name("Germany").code("DE").continent(Continent.EUROPE).build();

            when(countryService.createCountry(any(CountryRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(post("/api/countries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should return 400 on invalid request")
        void shouldRejectInvalid() throws Exception {
            CountryRequest request = CountryRequest.builder().build();

            mockMvc.perform(post("/api/countries")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/countries/{id}")
    class Update {

        @Test
        @DisplayName("should update country and return 200")
        void shouldUpdate() throws Exception {
            CountryRequest request = CountryRequest.builder()
                    .name("Ukraine Updated").code("UA").continent(Continent.EUROPE).build();

            when(countryService.updateCountry(eq(1L), any(CountryRequest.class)))
                    .thenReturn(buildResponse());

            mockMvc.perform(put("/api/countries/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/countries/{id}")
    class Delete {

        @Test
        @DisplayName("should delete and return 204")
        void shouldDelete() throws Exception {
            mockMvc.perform(delete("/api/countries/1"))
                    .andExpect(status().isNoContent());

            verify(countryService).deleteCountry(1L);
        }
    }
}