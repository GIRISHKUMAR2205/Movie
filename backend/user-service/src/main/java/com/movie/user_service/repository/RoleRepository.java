package com.movie.user_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.movie.user_service.entity.Role;

public interface RoleRepository extends JpaRepository<Role,Long>{

    Role findByRoleName(String roleName);

    
    
}
