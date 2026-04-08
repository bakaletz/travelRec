package com.travelRec.entity;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CityTest {

    private City buildCity(float culture, float food, float nightlife,
                           float nature, float safety, float cost,
                           float beach, float architecture, float shopping) {
        return City.builder()
                .name("Test City")
                .cityType(CityType.LARGE_CITY)
                .climateType(ClimateType.TEMPERATE)
                .cultureScore(culture)
                .foodScore(food)
                .nightlifeScore(nightlife)
                .natureScore(nature)
                .safetyScore(safety)
                .costLevel(cost)
                .beachScore(beach)
                .architectureScore(architecture)
                .shoppingScore(shopping)
                .baseCultureScore(culture)
                .baseFoodScore(food)
                .baseNightlifeScore(nightlife)
                .baseNatureScore(nature)
                .baseSafetyScore(safety)
                .baseCostLevel(cost)
                .baseBeachScore(beach)
                .baseArchitectureScore(architecture)
                .baseShoppingScore(shopping)
                .publicTransportScore(0.5f)
                .walkabilityScore(0.5f)
                .popularity(0.0f)
                .ratingCount(0)
                .build();
    }

    @Nested
    @DisplayName("toVector()")
    class ToVector {

        @Test
        @DisplayName("should return array of length 9")
        void shouldReturnCorrectLength() {
            City city = buildCity(0.8f, 0.7f, 0.9f, 0.6f, 0.7f, 0.4f, 0.2f, 0.8f, 0.5f);
            double[] vector = city.toVector();
            assertEquals(9, vector.length);
        }

        @Test
        @DisplayName("should return scores in correct order")
        void shouldReturnCorrectOrder() {
            City city = buildCity(0.85f, 0.80f, 0.90f, 0.60f, 0.70f, 0.35f, 0.15f, 0.90f, 0.50f);
            double[] vector = city.toVector();

            assertEquals(0.85, vector[0], 0.01); // culture
            assertEquals(0.80, vector[1], 0.01); // food
            assertEquals(0.90, vector[2], 0.01); // nightlife
            assertEquals(0.60, vector[3], 0.01); // nature
            assertEquals(0.70, vector[4], 0.01); // safety
            assertEquals(0.35, vector[5], 0.01); // cost
            assertEquals(0.15, vector[6], 0.01); // beach
            assertEquals(0.90, vector[7], 0.01); // architecture
            assertEquals(0.50, vector[8], 0.01); // shopping
        }

        @Test
        @DisplayName("should handle zero scores")
        void shouldHandleZeros() {
            City city = buildCity(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
            double[] vector = city.toVector();

            for (double v : vector) {
                assertEquals(0.0, v, 0.001);
            }
        }

        @Test
        @DisplayName("should handle max scores")
        void shouldHandleMaxScores() {
            City city = buildCity(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f);
            double[] vector = city.toVector();

            for (double v : vector) {
                assertEquals(1.0, v, 0.001);
            }
        }
    }

    @Nested
    @DisplayName("initCalculatedScores()")
    class InitCalculatedScores {

        @Test
        @DisplayName("should copy base scores to calculated when null")
        void shouldCopyBaseScores() {
            City city = City.builder()
                    .name("Test")
                    .cityType(CityType.MEDIUM_CITY)
                    .climateType(ClimateType.CONTINENTAL)
                    .baseCostLevel(0.4f)
                    .baseSafetyScore(0.7f)
                    .baseCultureScore(0.8f)
                    .baseFoodScore(0.6f)
                    .baseNightlifeScore(0.5f)
                    .baseNatureScore(0.9f)
                    .baseBeachScore(0.1f)
                    .baseArchitectureScore(0.7f)
                    .baseShoppingScore(0.3f)
                    .publicTransportScore(0.6f)
                    .walkabilityScore(0.8f)
                    .build();

            city.initCalculatedScores();

            assertEquals(0.4f, city.getCostLevel());
            assertEquals(0.7f, city.getSafetyScore());
            assertEquals(0.8f, city.getCultureScore());
            assertEquals(0.6f, city.getFoodScore());
            assertEquals(0.5f, city.getNightlifeScore());
            assertEquals(0.9f, city.getNatureScore());
            assertEquals(0.1f, city.getBeachScore());
            assertEquals(0.7f, city.getArchitectureScore());
            assertEquals(0.3f, city.getShoppingScore());
            assertEquals(0.0f, city.getPopularity());
            assertEquals(0, city.getRatingCount());
        }

        @Test
        @DisplayName("should not overwrite existing calculated scores")
        void shouldNotOverwriteExisting() {
            City city = City.builder()
                    .name("Test")
                    .cityType(CityType.MEGAPOLIS)
                    .climateType(ClimateType.TROPICAL)
                    .baseCultureScore(0.5f)
                    .baseFoodScore(0.5f)
                    .baseNightlifeScore(0.5f)
                    .baseNatureScore(0.5f)
                    .baseSafetyScore(0.5f)
                    .baseCostLevel(0.5f)
                    .baseBeachScore(0.5f)
                    .baseArchitectureScore(0.5f)
                    .baseShoppingScore(0.5f)
                    .publicTransportScore(0.5f)
                    .walkabilityScore(0.5f)
                    .cultureScore(0.9f)
                    .foodScore(0.8f)
                    .build();

            city.initCalculatedScores();

            assertEquals(0.9f, city.getCultureScore());
            assertEquals(0.8f, city.getFoodScore());
        }
    }
}
