package com.travelRec.util;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;

import java.util.EnumMap;
import java.util.Map;

public final class ContextDistance {

    private ContextDistance() {}

    private static final Map<ClimateType, double[]> CLIMATE_COORDS = new EnumMap<>(ClimateType.class);
    private static final Map<Continent, double[]> CONTINENT_COORDS = new EnumMap<>(Continent.class);
    private static final Map<CityType, Double> CITY_TYPE_URBANIZATION = new EnumMap<>(CityType.class);

    private static final double MAX_CLIMATE_DISTANCE;
    private static final double MAX_CONTINENT_DISTANCE;

    static {
        CLIMATE_COORDS.put(ClimateType.POLAR,         new double[]{0.00, 0.20});
        CLIMATE_COORDS.put(ClimateType.CONTINENTAL,   new double[]{0.30, 0.40});
        CLIMATE_COORDS.put(ClimateType.TEMPERATE,     new double[]{0.50, 0.50});
        CLIMATE_COORDS.put(ClimateType.OCEANIC,       new double[]{0.50, 0.80});
        CLIMATE_COORDS.put(ClimateType.MEDITERRANEAN, new double[]{0.70, 0.30});
        CLIMATE_COORDS.put(ClimateType.DRY,           new double[]{0.60, 0.00});
        CLIMATE_COORDS.put(ClimateType.TROPICAL,      new double[]{1.00, 0.90});

        CONTINENT_COORDS.put(Continent.EUROPE,        new double[]{0.50, 0.70});
        CONTINENT_COORDS.put(Continent.EUROPE_ASIA,   new double[]{0.55, 0.70});
        CONTINENT_COORDS.put(Continent.ASIA,          new double[]{0.75, 0.60});
        CONTINENT_COORDS.put(Continent.AFRICA,        new double[]{0.50, 0.30});
        CONTINENT_COORDS.put(Continent.AFRICA_ASIA,   new double[]{0.60, 0.40});
        CONTINENT_COORDS.put(Continent.NORTH_AMERICA, new double[]{0.15, 0.75});
        CONTINENT_COORDS.put(Continent.SOUTH_AMERICA, new double[]{0.25, 0.20});
        CONTINENT_COORDS.put(Continent.OCEANIA,       new double[]{0.95, 0.15});
        CONTINENT_COORDS.put(Continent.ANTARCTICA,    new double[]{0.50, 0.00});

        CITY_TYPE_URBANIZATION.put(CityType.MEGAPOLIS,    1.00);
        CITY_TYPE_URBANIZATION.put(CityType.LARGE_CITY,   0.75);
        CITY_TYPE_URBANIZATION.put(CityType.MEDIUM_CITY,  0.50);
        CITY_TYPE_URBANIZATION.put(CityType.SMALL_TOWN,   0.25);
        CITY_TYPE_URBANIZATION.put(CityType.RESORT,       0.60);

        MAX_CLIMATE_DISTANCE = computeMaxDistance(CLIMATE_COORDS.values().toArray(double[][]::new));
        MAX_CONTINENT_DISTANCE = computeMaxDistance(CONTINENT_COORDS.values().toArray(double[][]::new));
    }

    public static double climateDistance(ClimateType a, ClimateType b) {
        if (a == null || b == null) return 0.0;
        if (a == b) return 0.0;
        double[] ca = CLIMATE_COORDS.get(a);
        double[] cb = CLIMATE_COORDS.get(b);
        return euclidean(ca, cb) / MAX_CLIMATE_DISTANCE;
    }

    public static double continentDistance(Continent a, Continent b) {
        if (a == null || b == null) return 0.0;
        if (a == b) return 0.0;
        double[] ca = CONTINENT_COORDS.get(a);
        double[] cb = CONTINENT_COORDS.get(b);
        return euclidean(ca, cb) / MAX_CONTINENT_DISTANCE;
    }

    public static double cityTypeDistance(CityType a, CityType b) {
        if (a == null || b == null) return 0.0;
        if (a == b) return 0.0;

        if (a == CityType.RESORT || b == CityType.RESORT) {
            CityType other = (a == CityType.RESORT) ? b : a;
            double otherUrban = CITY_TYPE_URBANIZATION.get(other);
            double resortUrban = CITY_TYPE_URBANIZATION.get(CityType.RESORT);
            return Math.max(0.5, Math.abs(otherUrban - resortUrban));
        }

        double ua = CITY_TYPE_URBANIZATION.get(a);
        double ub = CITY_TYPE_URBANIZATION.get(b);
        return Math.abs(ua - ub);
    }

    private static double euclidean(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    private static double computeMaxDistance(double[][] points) {
        double max = 0.0;
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                double d = euclidean(points[i], points[j]);
                if (d > max) max = d;
            }
        }
        return max;
    }
}