package com.movie.user_service.entity;

import java.util.HashSet;
import java.util.Set;


import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "privilege")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Privilege extends BaseEntity{

    @Column(name = "privilege_name", nullable = false, unique = true)
    private String privilegeName;

    @ManyToMany(mappedBy = "privileges")
    @JsonIgnore
    private Set<Role> roles = new HashSet<>();
}
