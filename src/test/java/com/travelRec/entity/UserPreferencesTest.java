package com.travelRec.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserPreferencesTest {

    @Nested
    @DisplayName("toVector()")
    class ToVector {

        @Test
        @DisplayName("should return array of length 9")
        void shouldReturnCorrectLength() {
            UserPreferences prefs = UserPreferences.builder().build();
            double[] vector = prefs.toVector();
            assertEquals(9, vector.length);
        }

        @Test
        @DisplayName("should return weights in same order as City.toVector()")
        void shouldMatchCityVectorOrder() {
            UserPreferences prefs = UserPreferences.builder()
                    .cultureWeight(0.9f)
                    .foodWeight(0.8f)
                    .nightlifeWeight(0.3f)
                    .natureWeight(0.7f)
                    .safetyWeight(0.2f)
                    .budgetWeight(0.6f)
                    .beachWeight(0.4f)
                    .architectureWeight(0.5f)
                    .shoppingWeight(0.1f)
                    .build();

            double[] vector = prefs.toVector();

            assertEquals(0.9, vector[0], 0.01); // culture
            assertEquals(0.8, vector[1], 0.01); // food
            assertEquals(0.3, vector[2], 0.01); // nightlife
            assertEquals(0.7, vector[3], 0.01); // nature
            assertEquals(0.2, vector[4], 0.01); // safety
            assertEquals(0.6, vector[5], 0.01); // budget
            assertEquals(0.4, vector[6], 0.01); // beach
            assertEquals(0.5, vector[7], 0.01); // architecture
            assertEquals(0.1, vector[8], 0.01); // shopping
        }

        @Test
        @DisplayName("should use default weights (0.5) when built with defaults")
        void shouldUseDefaults() {
            UserPreferences prefs = UserPreferences.builder().build();
            double[] vector = prefs.toVector();

            for (double v : vector) {
                assertEquals(0.5, v, 0.01);
            }
        }

        @Test
        @DisplayName("vector length should match City.toVector() length")
        void shouldMatchCityVectorLength() {
            UserPreferences prefs = UserPreferences.builder().build();

            City city = City.builder()
                    .cultureScore(0.5f).foodScore(0.5f).nightlifeScore(0.5f)
                    .natureScore(0.5f).safetyScore(0.5f).costLevel(0.5f)
                    .beachScore(0.5f).architectureScore(0.5f).shoppingScore(0.5f)
                    .baseCultureScore(0.5f).baseFoodScore(0.5f).baseNightlifeScore(0.5f)
                    .baseNatureScore(0.5f).baseSafetyScore(0.5f).baseCostLevel(0.5f)
                    .baseBeachScore(0.5f).baseArchitectureScore(0.5f).baseShoppingScore(0.5f)
                    .popularity(0f).ratingCount(0)
                    .build();

            assertEquals(prefs.toVector().length, city.toVector().length);
        }
    }
}
