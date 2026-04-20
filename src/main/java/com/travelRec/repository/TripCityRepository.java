package com.travelRec.repository;

import com.travelRec.entity.TripCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripCityRepository extends JpaRepository<TripCity, Long> {

    List<TripCity> findByTripIdOrderByVisitOrderAsc(Long tripId);

    boolean existsByTripIdAndCityId(Long tripId, Long cityId);

    void deleteByTripIdAndCityId(Long tripId, Long cityId);

    @Query("""
        SELECT COUNT(tc) FROM TripCity tc
        WHERE tc.trip.user.id = :userId
          AND tc.city.id = :cityId
          AND tc.trip.id <> :excludedTripId
          AND tc.trip.status IN (com.travelRec.entity.enums.TripStatus.COMPLETED,
                                 com.travelRec.entity.enums.TripStatus.RATED)
    """)
    long countOtherCompletedTripsWithCity(@Param("userId") Long userId,
                                          @Param("cityId") Long cityId,
                                          @Param("excludedTripId") Long excludedTripId);
}