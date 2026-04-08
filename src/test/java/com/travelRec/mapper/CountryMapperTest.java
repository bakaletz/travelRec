package com.travelRec.mapper;

import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.City;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.Continent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CountryMapperTest {

    private CountryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CountryMapper();
    }

    private CountryRequest buildRequest() {
        return CountryRequest.builder()
                .name("Ukraine")
                .code("UA")
                .continent(Continent.EUROPE)
                .language("Ukrainian")
                .currency("UAH")
                .description("A country in Eastern Europe")
                .imageUrl("https://example.com/ua.jpg")
                .build();
    }

    private Country buildCountry() {
        return Country.builder()
                .id(1L)
                .name("Ukraine")
                .code("UA")
                .continent(Continent.EUROPE)
                .language("Ukrainian")
                .currency("UAH")
                .description("A country in Eastern Europe")
                .imageUrl("https://example.com/ua.jpg")
                .cities(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("toEntity()")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from request")
        void shouldMapAllFields() {
            CountryRequest request = buildRequest();
            Country country = mapper.toEntity(request);

            assertEquals("Ukraine", country.getName());
            assertEquals("UA", country.getCode());
            assertEquals(Continent.EUROPE, country.getContinent());
            assertEquals("Ukrainian", country.getLanguage());
            assertEquals("UAH", country.getCurrency());
            assertEquals("A country in Eastern Europe", country.getDescription());
            assertEquals("https://example.com/ua.jpg", country.getImageUrl());
        }

        @Test
        @DisplayName("should not set id")
        void shouldNotSetId() {
            CountryRequest request = buildRequest();
            Country country = mapper.toEntity(request);

            assertNull(country.getId());
        }

        @Test
        @DisplayName("should handle null optional fields")
        void shouldHandleNullOptionalFields() {
            CountryRequest request = CountryRequest.builder()
                    .name("Test")
                    .code("TS")
                    .continent(Continent.ASIA)
                    .build();

            Country country = mapper.toEntity(request);

            assertEquals("Test", country.getName());
            assertNull(country.getLanguage());
            assertNull(country.getCurrency());
            assertNull(country.getDescription());
            assertNull(country.getImageUrl());
        }
    }

    @Nested
    @DisplayName("toResponse()")
    class ToResponse {

        @Test
        @DisplayName("should map all fields to response")
        void shouldMapAllFields() {
            Country country = buildCountry();
            CountryResponse response = mapper.toResponse(country);

            assertEquals(1L, response.getId());
            assertEquals("Ukraine", response.getName());
            assertEquals("UA", response.getCode());
            assertEquals(Continent.EUROPE, response.getContinent());
            assertEquals("Ukrainian", response.getLanguage());
            assertEquals("UAH", response.getCurrency());
            assertEquals("A country in Eastern Europe", response.getDescription());
            assertEquals("https://example.com/ua.jpg", response.getImageUrl());
        }

        @Test
        @DisplayName("should return correct city count")
        void shouldReturnCityCount() {
            Country country = buildCountry();
            country.setCities(List.of(
                    City.builder().name("Kyiv").build(),
                    City.builder().name("Lviv").build(),
                    City.builder().name("Odesa").build()
            ));

            CountryResponse response = mapper.toResponse(country);

            assertEquals(3, response.getCityCount());
        }

        @Test
        @DisplayName("should return zero city count for empty list")
        void shouldReturnZeroCityCount() {
            Country country = buildCountry();
            CountryResponse response = mapper.toResponse(country);

            assertEquals(0, response.getCityCount());
        }

        @Test
        @DisplayName("should return zero city count for null cities")
        void shouldReturnZeroCityCountForNull() {
            Country country = buildCountry();
            country.setCities(null);

            CountryResponse response = mapper.toResponse(country);

            assertEquals(0, response.getCityCount());
        }
    }

    @Nested
    @DisplayName("updateEntity()")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            Country country = buildCountry();
            CountryRequest request = CountryRequest.builder()
                    .name("Turkey")
                    .code("TR")
                    .continent(Continent.EUROPE_ASIA)
                    .language("Turkish")
                    .currency("TRY")
                    .description("Transcontinental country")
                    .imageUrl("https://example.com/tr.jpg")
                    .build();

            mapper.updateEntity(country, request);

            assertEquals("Turkey", country.getName());
            assertEquals("TR", country.getCode());
            assertEquals(Continent.EUROPE_ASIA, country.getContinent());
            assertEquals("Turkish", country.getLanguage());
            assertEquals("TRY", country.getCurrency());
            assertEquals("Transcontinental country", country.getDescription());
            assertEquals("https://example.com/tr.jpg", country.getImageUrl());
        }

        @Test
        @DisplayName("should preserve id after update")
        void shouldPreserveId() {
            Country country = buildCountry();
            CountryRequest request = buildRequest();
            request.setName("Updated");

            mapper.updateEntity(country, request);

            assertEquals(1L, country.getId());
        }
    }
}
