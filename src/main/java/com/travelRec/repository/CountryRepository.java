package com.travelRec.repository;

import com.travelRec.entity.Country;
import com.travelRec.entity.enums.Continent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByCode(String code);

    List<Country> findByContinent(Continent continent);

    boolean existsByName(String name);

    boolean existsByCode(String code);
}
