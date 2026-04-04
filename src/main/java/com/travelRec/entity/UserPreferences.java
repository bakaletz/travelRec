package com.travelRec.entity;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UserPreferences {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Builder.Default
    private Float cultureWeight = 0.5f;

    @Builder.Default
    private Float foodWeight = 0.5f;

    @Builder.Default
    private Float nightlifeWeight = 0.5f;

    @Builder.Default
    private Float natureWeight = 0.5f;

    @Builder.Default
    private Float safetyWeight = 0.5f;

    @Builder.Default
    private Float budgetWeight = 0.5f;

    @Builder.Default
    private Float beachWeight = 0.5f;

    @Builder.Default
    private Float architectureWeight = 0.5f;

    @Builder.Default
    private Float shoppingWeight = 0.5f;

    @Enumerated(EnumType.STRING)
    private CityType preferredCityType;

    @Enumerated(EnumType.STRING)
    private ClimateType preferredClimate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public double[] toVector() {
        return new double[]{
                cultureWeight,
                foodWeight,
                nightlifeWeight,
                natureWeight,
                safetyWeight,
                budgetWeight,
                beachWeight,
                architectureWeight,
                shoppingWeight
        };
    }
}
