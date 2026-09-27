package com.movie.user_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

import com.movie.user_service.entity.RoleRequest;
import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;

public interface RoleRequestRepository extends JpaRepository<RoleRequest,Long>{

    boolean existsByUserAndRequestStatusAndRequestedRole(User user, RequestStatus requestStatus,Role role);
    
    @Query("""
            SELECT rr FROM RoleRequest rr
            JOIN FETCH rr.user
            JOIN FETCH rr.requestedRole
            WHERE rr.id = :id
            """)
    Optional<RoleRequest> findByIdWithUserAndRequestedRole(@Param("id") Long id);

    @Query("""
            SELECT rr FROM RoleRequest rr
            JOIN FETCH rr.user
            JOIN FETCH rr.requestedRole
            WHERE rr.requestStatus = :status
            ORDER BY rr.createdAt ASC
            """)
    List<RoleRequest> findAllByRequestStatusWithUserAndRequestedRole(@Param("status") RequestStatus status);

    @Query("""
            SELECT rr FROM RoleRequest rr
            JOIN FETCH rr.requestedRole
            WHERE rr.user = :user
            ORDER BY rr.createdAt DESC
            """)
    List<RoleRequest> findAllByUserWithRequestedRoleOrderByCreatedAtDesc(@Param("user") User user);
} 
