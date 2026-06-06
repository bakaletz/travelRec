package com.travelRec.util;

public final class VectorMath {

    private VectorMath() {}

    public static double centeredCosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }

        double meanA = 0.0;
        double meanB = 0.0;
        for (int i = 0; i < a.length; i++) {
            meanA += a[i];
            meanB += b[i];
        }
        meanA /= a.length;
        meanB /= b.length;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            double ai = a[i] - meanA;
            double bi = b[i] - meanB;
            dotProduct += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return (similarity + 1.0) / 2.0;
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    public static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static float adaptiveEma(float currentWeight, double newRating, int priorRatingCount, double baseAlpha) {
        double alpha = Math.max(baseAlpha, 1.0 / (priorRatingCount + 1));
        double newWeight = (1.0 - alpha) * currentWeight + alpha * newRating;
        return (float) Math.max(0.0, Math.min(1.0, newWeight));
    }

    public static double weightedUtility(double[] weights, double[] features) {
        if (weights.length != features.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        double weighted = 0.0;
        double weightSum = 0.0;
        for (int i = 0; i < weights.length; i++) {
            weighted += weights[i] * features[i];
            weightSum += weights[i];
        }
        if (weightSum == 0.0) return 0.0;
        return weighted / weightSum;
    }
}