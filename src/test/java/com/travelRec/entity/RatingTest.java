package com.travelRec.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RatingTest {

    @Nested
    @DisplayName("isDetailed()")
    class IsDetailed {

        @Test
        @DisplayName("should return false when only overall score is set")
        void shouldReturnFalseForQuickRating() {
            Rating rating = Rating.builder()
                    .overallScore(5)
                    .build();

            assertFalse(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when culture rating is set")
        void shouldReturnTrueWithCulture() {
            Rating rating = Rating.builder()
                    .overallScore(5)
                    .cultureRating(4)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when food rating is set")
        void shouldReturnTrueWithFood() {
            Rating rating = Rating.builder()
                    .overallScore(3)
                    .foodRating(5)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when all category ratings are set")
        void shouldReturnTrueWithAllCategories() {
            Rating rating = Rating.builder()
                    .overallScore(4)
                    .cultureRating(5)
                    .foodRating(4)
                    .nightlifeRating(3)
                    .natureRating(4)
                    .safetyRating(5)
                    .costRating(3)
                    .beachRating(2)
                    .architectureRating(5)
                    .shoppingRating(3)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only nightlife rating is set")
        void shouldReturnTrueWithNightlife() {
            Rating rating = Rating.builder()
                    .overallScore(2)
                    .nightlifeRating(1)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only nature rating is set")
        void shouldReturnTrueWithNature() {
            Rating rating = Rating.builder()
                    .overallScore(4)
                    .natureRating(3)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only safety rating is set")
        void shouldReturnTrueWithSafety() {
            Rating rating = Rating.builder()
                    .overallScore(3)
                    .safetyRating(5)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only cost rating is set")
        void shouldReturnTrueWithCost() {
            Rating rating = Rating.builder()
                    .overallScore(4)
                    .costRating(2)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only beach rating is set")
        void shouldReturnTrueWithBeach() {
            Rating rating = Rating.builder()
                    .overallScore(5)
                    .beachRating(4)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only architecture rating is set")
        void shouldReturnTrueWithArchitecture() {
            Rating rating = Rating.builder()
                    .overallScore(4)
                    .architectureRating(5)
                    .build();

            assertTrue(rating.isDetailed());
        }

        @Test
        @DisplayName("should return true when only shopping rating is set")
        void shouldReturnTrueWithShopping() {
            Rating rating = Rating.builder()
                    .overallScore(3)
                    .shoppingRating(2)
                    .build();

            assertTrue(rating.isDetailed());
        }
    }

    @Nested
    @DisplayName("normalize()")
    class Normalize {

        @Test
        @DisplayName("should normalize 1 to 0.0")
        void shouldNormalizeMinToZero() {
            assertEquals(0.0, Rating.normalize(1), 0.001);
        }

        @Test
        @DisplayName("should normalize 5 to 1.0")
        void shouldNormalizeMaxToOne() {
            assertEquals(1.0, Rating.normalize(5), 0.001);
        }

        @Test
        @DisplayName("should normalize 3 to 0.5")
        void shouldNormalizeMidToHalf() {
            assertEquals(0.5, Rating.normalize(3), 0.001);
        }

        @Test
        @DisplayName("should normalize 2 to 0.25")
        void shouldNormalizeTwoToQuarter() {
            assertEquals(0.25, Rating.normalize(2), 0.001);
        }

        @Test
        @DisplayName("should normalize 4 to 0.75")
        void shouldNormalizeFourToThreeQuarters() {
            assertEquals(0.75, Rating.normalize(4), 0.001);
        }
    }
}