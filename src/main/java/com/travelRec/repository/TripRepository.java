package com.travelRec.repository;

import com.travelRec.entity.Trip;
import com.travelRec.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserId(Long userId);

    List<Trip> findByUserIdAndStatus(Long userId, TripStatus status);

    List<Trip> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TripStatus status);

    List<Trip> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT t FROM Trip t
        WHERE t.user.id = :userId
          AND t.id <> :excludedTripId
          AND t.status IN (com.travelRec.entity.enums.TripStatus.COMPLETED,
                           com.travelRec.entity.enums.TripStatus.RATED)
          AND t.startDate < :endDate
          AND t.endDate > :startDate
    """)
    List<Trip> findOverlappingTrips(@Param("userId") Long userId,
                                    @Param("excludedTripId") Long excludedTripId,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);
}