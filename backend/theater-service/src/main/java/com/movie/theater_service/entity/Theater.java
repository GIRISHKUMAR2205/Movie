package com.movie.theater_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "theaters")
@Getter
@Setter
@NoArgsConstructor
public class Theater extends BaseEntity {
    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "owner_subject", nullable = false, length = 255)
    private String ownerSubject;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;
}
