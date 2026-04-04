package com.travelRec.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "trip_id", "city_id"}
        ))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    @Min(1) @Max(5)
    private Integer overallScore;

    @Min(1) @Max(5)
    private Integer cultureRating;

    @Min(1) @Max(5)
    private Integer foodRating;

    @Min(1) @Max(5)
    private Integer nightlifeRating;

    @Min(1) @Max(5)
    private Integer natureRating;

    @Min(1) @Max(5)
    private Integer safetyRating;

    @Min(1) @Max(5)
    private Integer costRating;

    @Min(1) @Max(5)
    private Integer beachRating;

    @Min(1) @Max(5)
    private Integer architectureRating;

    @Min(1) @Max(5)
    private Integer shoppingRating;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public boolean isDetailed() {
        return cultureRating != null
                || foodRating != null
                || nightlifeRating != null
                || natureRating != null
                || safetyRating != null
                || costRating != null
                || beachRating != null
                || architectureRating != null
                || shoppingRating != null;
    }

    public static double normalize(int rating) {
        return (rating - 1) / 4.0;
    }
}