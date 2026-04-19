package com.travelRec.repository;

import com.travelRec.entity.Trip;
import com.travelRec.entity.enums.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserId(Long userId);

    List<Trip> findByUserIdAndStatus(Long userId, TripStatus status);

    List<Trip> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, TripStatus status);

    List<Trip> findByUserIdOrderByCreatedAtDesc(Long userId);
}