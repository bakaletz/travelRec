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

    @Query("SELECT c FROM City c WHERE c.country.continent = :continent OR c.country.continent = CONCAT(:continent, '_ASIA') OR c.country.continent = CONCAT(:continent, '_EUROPE')")
    List<City> findByContinentIncludingMixed(@Param("continent") String continent);

    List<City> findByRegion(String region);

    @Query("SELECT c FROM City c ORDER BY c.popularity DESC LIMIT :limit")
    List<City> findTopByPopularity(@Param("limit") int limit);

    @Query("SELECT c FROM City c WHERE c.costLevel <= :maxCost")
    List<City> findByCostLevelLessThanEqual(@Param("maxCost") Float maxCost);

    @Query("SELECT c FROM City c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(c.region) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<City> searchByNameOrRegion(@Param("query") String query);
}
