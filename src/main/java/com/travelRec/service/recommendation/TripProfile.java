package com.travelRec.service.recommendation;

import com.travelRec.entity.enums.CityType;

public record TripProfile(
        int targetCityCount,
        int targetDurationDays,
        int durationWindowHalf,
        CityType dominantCityType,
        DataBucket bucket
) {
    public enum DataBucket { COLD_START, SPARSE, RICH }

    public boolean isColdStart() {
        return bucket == DataBucket.COLD_START;
    }

    public int minDurationDays() {
        return Math.max(1, targetDurationDays - durationWindowHalf);
    }

    public int maxDurationDays() {
        return targetDurationDays + durationWindowHalf;
    }
}