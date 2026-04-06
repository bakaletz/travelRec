package com.travelRec.entity;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cities")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(nullable = false)
    private String name;

    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CityType cityType;

    private Integer population;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClimateType climateType;

    private Float avgTempSummer;
    private Float avgTempWinter;

    @Column(nullable = false)
    private Float baseCostLevel;

    @Column(nullable = false)
    private Float baseSafetyScore;

    @Column(nullable = false)
    private Float baseCultureScore;

    @Column(nullable = false)
    private Float baseFoodScore;

    @Column(nullable = false)
    private Float baseNightlifeScore;

    @Column(nullable = false)
    private Float baseNatureScore;

    @Column(nullable = false)
    private Float baseBeachScore;

    @Column(nullable = false)
    private Float baseArchitectureScore;

    @Column(nullable = false)
    private Float baseShoppingScore;

    @Column(nullable = false)
    private Float costLevel;

    @Column(nullable = false)
    private Float safetyScore;

    @Column(nullable = false)
    private Float cultureScore;

    @Column(nullable = false)
    private Float foodScore;

    @Column(nullable = false)
    private Float nightlifeScore;

    @Column(nullable = false)
    private Float natureScore;

    @Column(nullable = false)
    private Float beachScore;

    @Column(nullable = false)
    private Float architectureScore;

    @Column(nullable = false)
    private Float shoppingScore;

    @Column(nullable = false)
    private Float publicTransportScore;

    @Column(nullable = false)
    private Float walkabilityScore;

    @Column(nullable = false)
    private Float popularity;

    @Column(nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;


    public double[] toVector() {
        return new double[]{
                cultureScore,
                foodScore,
                nightlifeScore,
                natureScore,
                safetyScore,
                costLevel,
                beachScore,
                architectureScore,
                shoppingScore
        };
    }

    @PrePersist
    public void initCalculatedScores() {
        if (costLevel == null) costLevel = baseCostLevel;
        if (safetyScore == null) safetyScore = baseSafetyScore;
        if (cultureScore == null) cultureScore = baseCultureScore;
        if (foodScore == null) foodScore = baseFoodScore;
        if (nightlifeScore == null) nightlifeScore = baseNightlifeScore;
        if (natureScore == null) natureScore = baseNatureScore;
        if (beachScore == null) beachScore = baseBeachScore;
        if (architectureScore == null) architectureScore = baseArchitectureScore;
        if (shoppingScore == null) shoppingScore = baseShoppingScore;
        if (popularity == null) popularity = 0.0f;
        if (ratingCount == null) ratingCount = 0;
    }
}
