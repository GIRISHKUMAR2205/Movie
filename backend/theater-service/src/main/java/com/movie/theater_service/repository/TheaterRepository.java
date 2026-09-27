package com.movie.theater_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.theater_service.entity.Theater;

public interface TheaterRepository extends JpaRepository<Theater, Long> {
    Optional<Theater> findByIdAndOwnerSubject(Long id, String ownerSubject);
    List<Theater> findAllByOwnerSubjectOrderByNameAsc(String ownerSubject);
}
