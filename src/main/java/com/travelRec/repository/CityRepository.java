package com.travelRec.repository;

import com.travelRec.entity.City;
import com.travelRec.entity.enums.CityType;
import com.travelRec.entity.enums.ClimateType;
import com.travelRec.entity.enums.Continent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByCountryId(Long countryId);

    List<City> findByCityType(CityType cityType);

    List<City> findByClimateType(ClimateType climateType);

    List<City> findByCountryContinent(Continent continent);

    List<City> findByRegion(String region);

    @Query("SELECT c FROM City c ORDER BY c.popularity DESC LIMIT :limit")
    List<City> findTopByPopularity(@Param("limit") int limit);

    @Query("SELECT c FROM City c WHERE c.costLevel <= :maxCost")
    List<City> findByCostLevelLessThanEqual(@Param("maxCost") Float maxCost);

    @Query("SELECT c FROM City c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.region) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<City> searchByNameOrRegion(@Param("query") String query);

    @Query(value = "SELECT *, " +
            "(6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "FROM cities WHERE id != :cityId " +
            "HAVING distance <= :radiusKm " +
            "ORDER BY distance ASC", nativeQuery = true)
    List<City> findNearbyCities(@Param("cityId") Long cityId,
                                @Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radiusKm") double radiusKm);

    @Query("SELECT c FROM City c WHERE c.country.id = :countryId AND c.id != :cityId")
    List<City> findByCountryIdExcluding(@Param("countryId") Long countryId,
                                        @Param("cityId") Long cityId);
}