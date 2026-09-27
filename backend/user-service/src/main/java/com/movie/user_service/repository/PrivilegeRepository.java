package com.movie.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.user_service.entity.Privilege;

public interface PrivilegeRepository extends JpaRepository<Privilege,Long> {
    
}
