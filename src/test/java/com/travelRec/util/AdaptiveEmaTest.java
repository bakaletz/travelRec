package com.travelRec.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveEmaTest {

    private static final double EPSILON = 1e-4;
    private static final double BASE_ALPHA = 0.1;

    @Nested
    @DisplayName("First rating (cold start)")
    class FirstRating {

        @Test
        @DisplayName("first rating fully replaces default weight (alpha = 1.0)")
        void firstRatingReplaces() {
            float result = VectorMath.adaptiveEma(0.5f, 0.75, 0, BASE_ALPHA);
            assertEquals(0.75f, result, EPSILON);
        }

        @Test
        @DisplayName("first rating of 0.0 drops weight to 0")
        void firstRatingZero() {
            float result = VectorMath.adaptiveEma(0.5f, 0.0, 0, BASE_ALPHA);
            assertEquals(0.0f, result, EPSILON);
        }

        @Test
        @DisplayName("first rating of 1.0 pulls weight to 1.0")
        void firstRatingMax() {
            float result = VectorMath.adaptiveEma(0.5f, 1.0, 0, BASE_ALPHA);
            assertEquals(1.0f, result, EPSILON);
        }
    }

    @Nested
    @DisplayName("Bootstrap phase (first 10 ratings)")
    class BootstrapPhase {

        @Test
        @DisplayName("second rating uses alpha = 0.5")
        void secondRatingHalfAlpha() {
            float result = VectorMath.adaptiveEma(0.75f, 0.25, 1, BASE_ALPHA);
            assertEquals(0.5f, result, EPSILON);
        }

        @Test
        @DisplayName("consistent ratings converge to their value in bootstrap")
        void convergesToMean() {
            float weight = 0.5f;
            for (int i = 0; i < 5; i++) {
                weight = VectorMath.adaptiveEma(weight, 0.8, i, BASE_ALPHA);
            }
            assertEquals(0.8f, weight, EPSILON);
        }

        @Test
        @DisplayName("alternating ratings converge to arithmetic mean")
        void alternatingRatings() {
            float weight = 0.5f;
            double[] ratings = {0.2, 0.8, 0.2, 0.8, 0.2};
            for (int i = 0; i < ratings.length; i++) {
                weight = VectorMath.adaptiveEma(weight, ratings[i], i, BASE_ALPHA);
            }
            double expectedMean = 0.44;
            assertEquals(expectedMean, weight, EPSILON);
        }
    }

    @Nested
    @DisplayName("Stable phase (after 10 ratings)")
    class StablePhase {

        @Test
        @DisplayName("alpha stabilizes at base value after 10 ratings")
        void alphaStabilizes() {
            float result1 = VectorMath.adaptiveEma(0.5f, 1.0, 10, BASE_ALPHA);
            float result2 = VectorMath.adaptiveEma(0.5f, 1.0, 100, BASE_ALPHA);
            assertEquals(result1, result2, EPSILON);
        }

        @Test
        @DisplayName("post-bootstrap update moves weight by ~10% toward new rating")
        void postBootstrapSlowUpdate() {
            float result = VectorMath.adaptiveEma(0.5f, 1.0, 50, BASE_ALPHA);
            assertEquals(0.55f, result, EPSILON);
        }

        @Test
        @DisplayName("many consistent ratings in stable phase slowly converge")
        void slowConvergenceInStablePhase() {
            float weight = 0.5f;
            for (int i = 10; i < 50; i++) {
                weight = VectorMath.adaptiveEma(weight, 0.9, i, BASE_ALPHA);
            }
            assertTrue(weight > 0.85f && weight < 0.9f,
                    "Expected weight between 0.85 and 0.9 after 40 iterations, got " + weight);
        }
    }

    @Nested
    @DisplayName("Invariants")
    class Invariants {

        @Test
        @DisplayName("result is always clamped to [0, 1]")
        void clampedToRange() {
            float result = VectorMath.adaptiveEma(0.99f, 1.0, 0, BASE_ALPHA);
            assertTrue(result >= 0.0f && result <= 1.0f);

            float result2 = VectorMath.adaptiveEma(0.01f, 0.0, 0, BASE_ALPHA);
            assertTrue(result2 >= 0.0f && result2 <= 1.0f);
        }

        @Test
        @DisplayName("identical rating leaves weight unchanged")
        void identicalRatingUnchanged() {
            float result = VectorMath.adaptiveEma(0.7f, 0.7, 5, BASE_ALPHA);
            assertEquals(0.7f, result, EPSILON);
        }
    }
}