package com.movie.user_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.user_service.entity.RoleRequest;
import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.User;

public interface RoleRequestRepository extends JpaRepository<RoleRequest,Long>{

    boolean existsByUserAndRequestStatusAndRequestedRole(User user, RequestStatus requestStatus,Role role);
    
    RoleRequest findByUser(User user);

    RoleRequest findByUserAndRequestStatus(User user,RequestStatus requestStatus);
} 
