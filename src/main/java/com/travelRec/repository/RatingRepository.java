package com.travelRec.repository;

import com.travelRec.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findByTripId(Long tripId);

    List<Rating> findByCityId(Long cityId);

    List<Rating> findByUserId(Long userId);

    Optional<Rating> findByUserIdAndTripIdAndCityId(Long userId, Long tripId, Long cityId);

    boolean existsByUserIdAndTripIdAndCityId(Long userId, Long tripId, Long cityId);

    @Query("SELECT AVG(r.cultureRating) FROM Rating r WHERE r.city.id = :cityId AND r.cultureRating IS NOT NULL")
    Optional<Double> avgCultureRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.foodRating) FROM Rating r WHERE r.city.id = :cityId AND r.foodRating IS NOT NULL")
    Optional<Double> avgFoodRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.nightlifeRating) FROM Rating r WHERE r.city.id = :cityId AND r.nightlifeRating IS NOT NULL")
    Optional<Double> avgNightlifeRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.natureRating) FROM Rating r WHERE r.city.id = :cityId AND r.natureRating IS NOT NULL")
    Optional<Double> avgNatureRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.safetyRating) FROM Rating r WHERE r.city.id = :cityId AND r.safetyRating IS NOT NULL")
    Optional<Double> avgSafetyRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.costRating) FROM Rating r WHERE r.city.id = :cityId AND r.costRating IS NOT NULL")
    Optional<Double> avgCostRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.beachRating) FROM Rating r WHERE r.city.id = :cityId AND r.beachRating IS NOT NULL")
    Optional<Double> avgBeachRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.architectureRating) FROM Rating r WHERE r.city.id = :cityId AND r.architectureRating IS NOT NULL")
    Optional<Double> avgArchitectureRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.shoppingRating) FROM Rating r WHERE r.city.id = :cityId AND r.shoppingRating IS NOT NULL")
    Optional<Double> avgShoppingRatingByCityId(@Param("cityId") Long cityId);

    @Query("SELECT AVG(r.overallScore) FROM Rating r WHERE r.city.id = :cityId")
    Optional<Double> avgOverallScoreByCityId(@Param("cityId") Long cityId);

    long countByCityId(Long cityId);
}
