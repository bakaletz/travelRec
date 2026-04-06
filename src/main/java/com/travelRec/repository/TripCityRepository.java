package com.travelRec.repository;

import com.travelRec.entity.TripCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripCityRepository extends JpaRepository<TripCity, Long> {

    List<TripCity> findByTripIdOrderByVisitOrderAsc(Long tripId);

    boolean existsByTripIdAndCityId(Long tripId, Long cityId);

    void deleteByTripIdAndCityId(Long tripId, Long cityId);
}
