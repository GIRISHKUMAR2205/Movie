package com.movie.user_service.service;

import com.movie.user_service.repository.RoleRepository;


import org.springframework.stereotype.Service;

import com.movie.user_service.entity.Role;
import com.movie.user_service.exceptions.ResourceNotFoundException;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getRole(String roleName) {
        Role role=roleRepository.findByRoleName(roleName);
        if(role == null) throw new ResourceNotFoundException("No role available:"+roleName);
        return role;
    }
    
}
