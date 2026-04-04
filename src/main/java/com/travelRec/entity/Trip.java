package com.travelRec.entity;

import com.travelRec.entity.enums.TripStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trips")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TripStatus status = TripStatus.PLANNED;

    private LocalDate startDate;

    private LocalDate endDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visitOrder ASC")
    @Builder.Default
    private List<TripCity> tripCities = new ArrayList<>();

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Rating> ratings = new ArrayList<>();

    public void complete() {
        if (this.status != TripStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED trips can be completed");
        }
        this.status = TripStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status != TripStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED trips can be cancelled");
        }
        this.status = TripStatus.CANCELLED;
    }

    public void markAsRated() {
        if (this.status != TripStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED trips can be rated");
        }
        this.status = TripStatus.RATED;
    }
}
