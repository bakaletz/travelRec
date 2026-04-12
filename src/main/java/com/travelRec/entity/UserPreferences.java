package com.travelRec.entity;

import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @ElementCollection(targetClass = CityType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferred_city_types", joinColumns = @JoinColumn(name = "preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "city_type")
    @Builder.Default
    private Set<CityType> preferredCityTypes = new HashSet<>();

    @ElementCollection(targetClass = ClimateType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_preferred_climate_types", joinColumns = @JoinColumn(name = "preferences_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "climate_type")
    @Builder.Default
    private Set<ClimateType> preferredClimateTypes = new HashSet<>();

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

    public boolean hasPreferredCityTypes() {
        return preferredCityTypes != null
                && !preferredCityTypes.isEmpty();
    }

    public boolean hasPreferredClimateTypes() {
        return preferredClimateTypes != null
                && !preferredClimateTypes.isEmpty();
    }

    public boolean matchesCityType(CityType type) {
        return !hasPreferredCityTypes() || preferredCityTypes.contains(type);
    }

    public boolean matchesClimateType(ClimateType type) {
        return !hasPreferredClimateTypes() || preferredClimateTypes.contains(type);
    }
}
