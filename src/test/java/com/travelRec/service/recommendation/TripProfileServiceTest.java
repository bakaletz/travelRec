package com.travelRec.service.recommendation;

import com.travelRec.entity.City;
import com.travelRec.entity.Rating;
import com.travelRec.entity.Trip;
import com.travelRec.entity.TripCity;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.TripStatus;
import com.travelRec.repository.TripRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripProfileServiceTest {

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripProfileService tripProfileService;

    private long citySeq = 1;

    private City city(CityType type, ClimateType climate, double lat, double lng) {
        return City.builder()
                .id(citySeq++)
                .name("City" + citySeq)
                .cityType(type)
                .climateType(climate)
                .latitude(lat)
                .longitude(lng)
                .build();
    }

    private TripCity tc(City c, int order) {
        return TripCity.builder().city(c).visitOrder(order).build();
    }

    private Trip trip(LocalDate start, LocalDate end, List<TripCity> cities, List<Rating> ratings) {
        return Trip.builder()
                .name("trip")
                .status(TripStatus.COMPLETED)
                .startDate(start)
                .endDate(end)
                .tripCities(cities)
                .ratings(ratings == null ? new ArrayList<>() : ratings)
                .build();
    }

    private void stubTrips(List<Trip> completed, List<Trip> rated) {
        when(tripRepository.findByUserIdAndStatus(1L, TripStatus.COMPLETED)).thenReturn(completed);
        when(tripRepository.findByUserIdAndStatus(1L, TripStatus.RATED)).thenReturn(rated);
    }

    @Test
    @DisplayName("cold start when the user has no trips")
    void coldStartNoTrips() {
        stubTrips(List.of(), List.of());

        TripProfile p = tripProfileService.buildProfile(1L);

        assertEquals(TripProfile.DataBucket.COLD_START, p.bucket());
        assertTrue(p.isColdStart());
        assertEquals(2, p.targetCityCount());
        assertEquals(7, p.targetDurationDays());
        assertNull(p.dominantCityType());
        assertNull(p.dominantClimateType());
        assertEquals(2000.0, p.proximitySaturationKm());
        assertTrue(p.minDurationDays() >= 1);
        assertEquals(13, p.maxDurationDays());
    }

    @Test
    @DisplayName("trips without cities are ignored (still cold start)")
    void coldStartWhenTripsHaveNoCities() {
        Trip empty = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(), new ArrayList<>());

        stubTrips(List.of(empty), List.of());

        assertTrue(tripProfileService.buildProfile(1L).isColdStart());
    }

    @Test
    @DisplayName("sparse bucket for three valid trips (odd median, one zero-length trip)")
    void sparseOddMedian() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 48.2, 16.3);

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 9),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());
        // zero-length trip: end == start -> duration not counted
        Trip t3 = trip(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 1),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());

        stubTrips(List.of(t1, t2, t3), List.of());

        TripProfile p = tripProfileService.buildProfile(1L);

        assertEquals(TripProfile.DataBucket.SPARSE, p.bucket());
        assertEquals(2, p.targetCityCount());
        assertTrue(p.targetDurationDays() > 0);
    }

    @Test
    @DisplayName("rich bucket for four valid trips mixing COMPLETED and RATED (even median)")
    void richEvenMedian() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);
        City b = city(CityType.RESORT, ClimateType.MEDITERRANEAN, 41.9, 12.5);

        List<Trip> completed = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            completed.add(trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                    new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>()));
        }
        Trip rated = trip(LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 9),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());

        stubTrips(completed, List.of(rated));

        assertEquals(TripProfile.DataBucket.RICH, tripProfileService.buildProfile(1L).bucket());
    }

    @Test
    @DisplayName("duration falls back to default when dates are missing")
    void defaultDurationWhenDatesMissing() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 48.2, 16.3);

        Trip t1 = trip(null, null, new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());
        Trip t2 = trip(null, null, new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());

        stubTrips(List.of(t1, t2), List.of());

        assertEquals(7, tripProfileService.buildProfile(1L).targetDurationDays());
    }

    @Test
    @DisplayName("ratings weight the dominant city/climate type")
    void ratingsWeightDominantType() {
        City resort = city(CityType.RESORT, ClimateType.MEDITERRANEAN, 41.9, 12.5);
        City largeCity = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);

        Rating highResort = Rating.builder().city(resort).overallScore(5).build();
        Rating lowCity = Rating.builder().city(largeCity).overallScore(1).build();
        Rating nullCity = Rating.builder().city(null).overallScore(5).build();     // skipped
        Rating nullScore = Rating.builder().city(resort).overallScore(null).build(); // skipped

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(resort, 1), tc(largeCity, 2))),
                new ArrayList<>(List.of(highResort, lowCity, nullCity, nullScore)));
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(resort, 1), tc(largeCity, 2))),
                new ArrayList<>(List.of(highResort)));

        stubTrips(List.of(t1, t2), List.of());

        TripProfile p = tripProfileService.buildProfile(1L);

        assertEquals(CityType.RESORT, p.dominantCityType());
        assertEquals(ClimateType.MEDITERRANEAN, p.dominantClimateType());
    }

    @Test
    @DisplayName("cities with null type or null coordinates are skipped without error")
    void skipsNullTypeAndNullCoords() {
        City normal = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);
        City nullType = City.builder().id(900L).latitude(48.0).longitude(16.0).build();   // null type+climate
        City nullCoords = City.builder().id(901L)
                .cityType(CityType.LARGE_CITY).climateType(ClimateType.TEMPERATE).build(); // null lat/lng

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(normal, 1), tc(nullType, 2), tc(nullCoords, 3))),
                new ArrayList<>());
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(normal, 1), tc(nullType, 2), tc(nullCoords, 3))),
                new ArrayList<>());

        stubTrips(List.of(t1, t2), List.of());

        TripProfile p = tripProfileService.buildProfile(1L);

        assertEquals(TripProfile.DataBucket.SPARSE, p.bucket());
        assertEquals(CityType.LARGE_CITY, p.dominantCityType());
    }

    @Test
    @DisplayName("proximity saturation clamps to the minimum for very close cities (even leg count)")
    void saturationClampMin() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.50000, 19.00000);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.50050, 19.00050);

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());

        stubTrips(List.of(t1, t2), List.of());

        assertEquals(500.0, tripProfileService.buildProfile(1L).proximitySaturationKm());
    }

    @Test
    @DisplayName("proximity saturation clamps to the maximum for very distant cities")
    void saturationClampMax() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 0.0, 0.0);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 0.0, 80.0); // ~8900 km

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());

        stubTrips(List.of(t1, t2), List.of());

        assertEquals(5000.0, tripProfileService.buildProfile(1L).proximitySaturationKm());
    }

    @Test
    @DisplayName("proximity saturation handles an odd number of legs")
    void saturationOddLegMedian() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 0.0, 0.0);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 0.0, 5.0);
        City c = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 0.0, 12.0);

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2), tc(c, 3))), new ArrayList<>()); // 2 legs
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(a, 1), tc(b, 2))), new ArrayList<>());            // 1 leg => 3 legs total

        stubTrips(List.of(t1, t2), List.of());

        double saturation = tripProfileService.buildProfile(1L).proximitySaturationKm();
        assertTrue(saturation >= 500.0 && saturation <= 5000.0);
    }

    @Test
    @DisplayName("proximity saturation falls back to default when there are no legs")
    void saturationDefaultWhenSingleCityTrips() {
        City a = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 47.5, 19.0);
        City b = city(CityType.LARGE_CITY, ClimateType.TEMPERATE, 48.2, 16.3);

        Trip t1 = trip(LocalDate.of(2025, 5, 1), LocalDate.of(2025, 5, 6),
                new ArrayList<>(List.of(tc(a, 1))), new ArrayList<>());
        Trip t2 = trip(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 6),
                new ArrayList<>(List.of(tc(b, 1))), new ArrayList<>());

        stubTrips(List.of(t1, t2), List.of());

        assertEquals(2000.0, tripProfileService.buildProfile(1L).proximitySaturationKm());
    }
}