package com.movie.user_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String userName;

    @Column(nullable = false)
    private String email;

    private String password;

    @Column(nullable = false)
    private String role;

    // @Column(name = "refresh_token_hash",nullable = false)
    // private String refreshTokenHash;

    // @Column(name = "refresh_token_expires_at",nullable = false)
    // private Instant refreshTokenExpiresAt;

    @OneToMany(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @JsonManagedReference
    private List<OAuthAccount> oauthAccounts = new ArrayList<>();
}