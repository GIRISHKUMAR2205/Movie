package com.movie.user_service.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "oauth_accounts",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"provider", "provider_subject"}
        )
    }
)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Data
public class OAuthAccount extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "provider" , nullable = false)
    private AuthProvider authProvider;

    @Column(name = "provider_subject", nullable = false)
    private String providerSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
}