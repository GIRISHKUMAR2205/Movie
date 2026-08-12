package com.movie.user_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email"), @UniqueConstraint(columnNames ={"auth_provider","provider_id"} )
})
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long userId;
    String userName;
    String email;
    String password;
    AuthProvider authProvider;
    String providerId;
    String role;
}
