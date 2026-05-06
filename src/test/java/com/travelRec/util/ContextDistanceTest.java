package com.travelRec.util;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextDistanceTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("climateDistance")
    class ClimateDistance {

        @Test
        @DisplayName("same climate returns 0")
        void sameClimateZero() {
            for (ClimateType type : ClimateType.values()) {
                assertEquals(0.0, ContextDistance.climateDistance(type, type), EPSILON);
            }
        }

        @Test
        @DisplayName("different climates return value in (0, 1]")
        void differentClimatesInRange() {
            double dist = ContextDistance.climateDistance(ClimateType.TROPICAL, ClimateType.POLAR);
            assertTrue(dist > 0.0 && dist <= 1.0);
        }

        @Test
        @DisplayName("TROPICAL vs POLAR is the maximum distance (= 1.0)")
        void tropicalPolarIsMaximum() {
            double maxDist = ContextDistance.climateDistance(ClimateType.TROPICAL, ClimateType.POLAR);
            assertEquals(1.0, maxDist, EPSILON);
        }

        @Test
        @DisplayName("TEMPERATE and OCEANIC are close")
        void temperateOceanicClose() {
            double dist = ContextDistance.climateDistance(ClimateType.TEMPERATE, ClimateType.OCEANIC);
            assertTrue(dist < 0.3);
        }

        @Test
        @DisplayName("TEMPERATE is closer to OCEANIC than to TROPICAL")
        void temperateCloserToOceanic() {
            double toOceanic = ContextDistance.climateDistance(ClimateType.TEMPERATE, ClimateType.OCEANIC);
            double toTropical = ContextDistance.climateDistance(ClimateType.TEMPERATE, ClimateType.TROPICAL);
            assertTrue(toOceanic < toTropical);
        }

        @Test
        @DisplayName("is symmetric")
        void symmetric() {
            double ab = ContextDistance.climateDistance(ClimateType.DRY, ClimateType.MEDITERRANEAN);
            double ba = ContextDistance.climateDistance(ClimateType.MEDITERRANEAN, ClimateType.DRY);
            assertEquals(ab, ba, EPSILON);
        }

        @Test
        @DisplayName("null input returns 0")
        void nullReturnsZero() {
            assertEquals(0.0, ContextDistance.climateDistance(null, ClimateType.TROPICAL));
            assertEquals(0.0, ContextDistance.climateDistance(ClimateType.TROPICAL, null));
        }
    }

    @Nested
    @DisplayName("continentDistance")
    class ContinentDistance {

        @Test
        @DisplayName("same continent returns 0")
        void sameContinentZero() {
            assertEquals(0.0, ContextDistance.continentDistance(Continent.EUROPE, Continent.EUROPE), EPSILON);
        }

        @Test
        @DisplayName("EUROPE is closer to ASIA than to OCEANIA")
        void europeCloserToAsiaThanOceania() {
            double toAsia = ContextDistance.continentDistance(Continent.EUROPE, Continent.ASIA);
            double toOceania = ContextDistance.continentDistance(Continent.EUROPE, Continent.OCEANIA);
            assertTrue(toAsia < toOceania);
        }

        @Test
        @DisplayName("EUROPE is very close to EUROPE_ASIA")
        void europeVeryCloseToEurasia() {
            double dist = ContextDistance.continentDistance(Continent.EUROPE, Continent.EUROPE_ASIA);
            assertTrue(dist < 0.1);
        }

        @Test
        @DisplayName("all distances are in [0, 1]")
        void allInRange() {
            for (Continent a : Continent.values()) {
                for (Continent b : Continent.values()) {
                    double d = ContextDistance.continentDistance(a, b);
                    assertTrue(d >= 0.0 && d <= 1.0,
                            a + " → " + b + " = " + d);
                }
            }
        }
    }

    @Nested
    @DisplayName("cityTypeDistance")
    class CityTypeDistanceTest {

        @Test
        @DisplayName("same type returns 0")
        void sameTypeZero() {
            assertEquals(0.0, ContextDistance.cityTypeDistance(CityType.MEGAPOLIS, CityType.MEGAPOLIS));
        }

        @Test
        @DisplayName("SMALL_TOWN and MEDIUM_CITY are adjacent")
        void smallToMediumIsSmall() {
            double dist = ContextDistance.cityTypeDistance(CityType.SMALL_TOWN, CityType.MEDIUM_CITY);
            assertEquals(0.25, dist, EPSILON);
        }

        @Test
        @DisplayName("SMALL_TOWN and MEGAPOLIS are far apart")
        void smallToMegaIsLarge() {
            double dist = ContextDistance.cityTypeDistance(CityType.SMALL_TOWN, CityType.MEGAPOLIS);
            assertEquals(0.75, dist, EPSILON);
        }

        @Test
        @DisplayName("RESORT vs MEGAPOLIS has minimum 0.5 distance")
        void resortVsMegapolisIsSignificant() {
            double dist = ContextDistance.cityTypeDistance(CityType.RESORT, CityType.MEGAPOLIS);
            assertTrue(dist >= 0.5);
        }

        @Test
        @DisplayName("is symmetric")
        void symmetric() {
            double ab = ContextDistance.cityTypeDistance(CityType.RESORT, CityType.LARGE_CITY);
            double ba = ContextDistance.cityTypeDistance(CityType.LARGE_CITY, CityType.RESORT);
            assertEquals(ab, ba, EPSILON);
        }
    }
}