package com.travelRec.entity;

import com.travelRec.entity.enums.Continent;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "countries")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Continent continent;

    private String language;

    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<City> cities = new ArrayList<>();
}
