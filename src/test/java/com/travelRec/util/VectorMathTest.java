package com.travelRec.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VectorMathTest {

    private static final double EPSILON = 1e-6;

    @Nested
    @DisplayName("centeredCosineSimilarity")
    class CenteredCosineSimilarity {

        @Test
        @DisplayName("identical vectors produce 1.0 (upper bound)")
        void identicalVectorsReachUpperBound() {
            double[] v = {0.3, 0.9, 0.1, 0.7};
            assertEquals(1.0, VectorMath.centeredCosineSimilarity(v, v), EPSILON);
        }

        @Test
        @DisplayName("result is always in [0, 1]")
        void resultInRange() {
            double[] a = {0.9, 0.1, 0.1, 0.9};
            double[] b = {0.1, 0.9, 0.9, 0.1};
            double result = VectorMath.centeredCosineSimilarity(a, b);
            assertTrue(result >= 0.0 && result <= 1.0,
                    "Expected result in [0, 1], got " + result);
        }

        @Test
        @DisplayName("opposing patterns produce 0.0 (lower bound)")
        void opposingPatternsReachLowerBound() {
            double[] a = {1.0, 0.0, 1.0, 0.0};
            double[] b = {0.0, 1.0, 0.0, 1.0};
            assertEquals(0.0, VectorMath.centeredCosineSimilarity(a, b), EPSILON);
        }
    }

    @Nested
    @DisplayName("haversineDistance")
    class HaversineDistance {

        @Test
        @DisplayName("same point produces 0 km")
        void samePointZeroDistance() {
            assertEquals(0.0, VectorMath.haversineDistance(49.55, 25.59, 49.55, 25.59), EPSILON);
        }

        @Test
        @DisplayName("Kyiv to Lviv is approximately 469 km")
        void kyivToLviv() {
            double distance = VectorMath.haversineDistance(50.45, 30.52, 49.84, 24.03);
            assertEquals(469.0, distance, 5.0);
        }

        @Test
        @DisplayName("antipodal points are approximately 20015 km apart")
        void antipodal() {
            double distance = VectorMath.haversineDistance(0.0, 0.0, 0.0, 180.0);
            assertEquals(20015.0, distance, 10.0);
        }
    }

    @Nested
    @DisplayName("round3")
    class Round3 {

        @Test
        @DisplayName("rounds to 3 decimal places")
        void roundsThreeDecimals() {
            assertEquals(0.877, VectorMath.round3(0.8765432));
            assertEquals(0.877, VectorMath.round3(0.8774999));
            assertEquals(0.878, VectorMath.round3(0.8775));
        }

        @Test
        @DisplayName("handles negative values")
        void handlesNegative() {
            assertEquals(-0.123, VectorMath.round3(-0.1234567));
        }
    }
}