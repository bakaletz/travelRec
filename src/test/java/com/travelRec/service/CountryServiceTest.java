package com.travelRec.service;

import com.travelRec.dto.country.CountryRequest;
import com.travelRec.dto.country.CountryResponse;
import com.travelRec.entity.Country;
import com.travelRec.entity.enums.Continent;
import com.travelRec.mapper.CountryMapper;
import com.travelRec.repository.CountryRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Spy
    private CountryMapper countryMapper = new CountryMapper();

    @InjectMocks
    private CountryService countryService;

    private Country country;

    @BeforeEach
    void setUp() {
        country = Country.builder()
                .id(1L).name("Ukraine").code("UA")
                .continent(Continent.EUROPE).language("Ukrainian").currency("UAH")
                .cities(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("getCountryById()")
    class GetCountryById {

        @Test
        @DisplayName("should return country response")
        void shouldReturn() {
            when(countryRepository.findById(1L)).thenReturn(Optional.of(country));

            CountryResponse response = countryService.getCountryById(1L);

            assertEquals("Ukraine", response.getName());
            assertEquals("UA", response.getCode());
            assertEquals(Continent.EUROPE, response.getContinent());
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(countryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> countryService.getCountryById(99L));
        }
    }

    @Nested
    @DisplayName("createCountry()")
    class CreateCountry {

        @Test
        @DisplayName("should create country")
        void shouldCreate() {
            CountryRequest request = CountryRequest.builder()
                    .name("Germany").code("DE").continent(Continent.EUROPE)
                    .language("German").currency("EUR").build();

            when(countryRepository.existsByName("Germany")).thenReturn(false);
            when(countryRepository.existsByCode("DE")).thenReturn(false);
            when(countryRepository.save(any(Country.class))).thenAnswer(inv -> {
                Country c = inv.getArgument(0);
                c.setId(2L);
                return c;
            });

            CountryResponse response = countryService.createCountry(request);

            assertEquals("Germany", response.getName());
            verify(countryRepository).save(any(Country.class));
        }

        @Test
        @DisplayName("should throw when name exists")
        void shouldThrowDuplicateName() {
            CountryRequest request = CountryRequest.builder()
                    .name("Ukraine").code("XX").continent(Continent.EUROPE).build();

            when(countryRepository.existsByName("Ukraine")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> countryService.createCountry(request));
        }

        @Test
        @DisplayName("should throw when code exists")
        void shouldThrowDuplicateCode() {
            CountryRequest request = CountryRequest.builder()
                    .name("New Country").code("UA").continent(Continent.EUROPE).build();

            when(countryRepository.existsByName("New Country")).thenReturn(false);
            when(countryRepository.existsByCode("UA")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () -> countryService.createCountry(request));
        }
    }

    @Nested
    @DisplayName("getCountriesByContinent()")
    class GetByContinent {

        @Test
        @DisplayName("should return filtered list")
        void shouldFilter() {
            when(countryRepository.findByContinent(Continent.EUROPE)).thenReturn(List.of(country));

            List<CountryResponse> results = countryService.getCountriesByContinent(Continent.EUROPE);

            assertEquals(1, results.size());
            assertEquals("Ukraine", results.get(0).getName());
        }

        @Test
        @DisplayName("should return empty for no matches")
        void shouldReturnEmpty() {
            when(countryRepository.findByContinent(Continent.ANTARCTICA)).thenReturn(List.of());

            List<CountryResponse> results = countryService.getCountriesByContinent(Continent.ANTARCTICA);

            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("deleteCountry()")
    class DeleteCountry {

        @Test
        @DisplayName("should delete existing country")
        void shouldDelete() {
            when(countryRepository.findById(1L)).thenReturn(Optional.of(country));

            countryService.deleteCountry(1L);

            verify(countryRepository).delete(country);
        }

        @Test
        @DisplayName("should throw when not found")
        void shouldThrow() {
            when(countryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> countryService.deleteCountry(99L));
        }
    }
}
