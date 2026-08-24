package com.movie.user_service.service;

import com.movie.user_service.repository.RoleAuditRepository;
import com.movie.user_service.repository.RoleRequestRepository;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.entity.RoleRequest;
import com.movie.user_service.entity.RoleStatus;
import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.RoleAudit;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleRequestService {
    private final RoleRequestRepository roleRequestRepository;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final RoleAuditRepository roleAuditRepository;

    @Transactional
    public void elevate(RoleDto roleDto) {
        User user = userRepository.findById(roleDto.getUserId()).orElseThrow( () -> new UsernameNotFoundException("No User exists"));
        //Create roles to be added 
        RoleRequest roleRequest = roleRequestRepository.findByUserAndRequestStatus(user,RequestStatus.PENDING);

        Role requestedRole = roleRequest.getRequestedRole();

        //Add to existing roles
        user.getRoles().add(requestedRole);
        
        changeRequestStatus(requestedRole,RequestStatus.APPROVED,RoleStatus.ACTIVE,roleRequest,roleDto.getReason());
        
    }

    public void reject(RoleDto roleDto) {
        User user = userRepository.findById(roleDto.getUserId()).orElseThrow( () -> new UsernameNotFoundException("No User exists"));

        RoleRequest roleRequest = roleRequestRepository.findByUserAndRequestStatus(user,RequestStatus.PENDING);

        Role requestedRole=roleRequest.getRequestedRole();

        changeRequestStatus(requestedRole,RequestStatus.REJECTED,roleRequest.getRoleStatus(),roleRequest,roleDto.getReason());
       
    }

    public void revoke(RoleDto roleDto) {

        User user = userRepository.findById(roleDto.getUserId()).orElseThrow( () -> new UsernameNotFoundException("No User exists"));
        RoleRequest roleRequest = roleRequestRepository.findByUser(user);
        Role requestedRole = roleRequest.getRequestedRole();

        user.getRoles().remove(requestedRole);
        changeRequestStatus(requestedRole, RequestStatus.REVOKED, RoleStatus.REVOKED, roleRequest, roleDto.getReason());
    }


    public void request(RoleDto roleDto) {
        User user=userRepository.findById(roleDto.getUserId()).orElseThrow(()-> new UsernameNotFoundException("No User found"));
        Role requestedRole = roleService.getRole(roleDto.getRoleName());
        if (roleRequestRepository.existsByUserAndRequestStatusAndRequestedRole(
                user,
                RequestStatus.PENDING,requestedRole)) {
            throw new AlreadyExistsException(requestedRole + " Request Already exists");
        }

        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRequestedRole(requestedRole);
        roleRequest.setUser(user);
        roleRequestRepository.save(roleRequest);
    }

    public void reactivate(RoleDto roleDto) {

        User user = userRepository.findById(roleDto.getUserId()).orElseThrow( () -> new UsernameNotFoundException("No User exists"));
        RoleRequest roleRequest = roleRequestRepository.findByUserAndRequestStatus(user,RequestStatus.REVOKED);
        Role requestedRole = roleRequest.getRequestedRole();
        
        changeRequestStatus(requestedRole, RequestStatus.REACTIVATED, RoleStatus.ACTIVE, roleRequest, roleDto.getReason());
    }


    private void changeRequestStatus(Role role,RequestStatus currRequestStatus,
            RoleStatus roleStatus, RoleRequest roleRequest, String reason) {
        
        RequestStatus prevRequestStatus=roleRequest.getRequestStatus();

        roleRequest.setRequestStatus(currRequestStatus);
        roleRequest.setRoleStatus(roleStatus);
        
        RoleAudit roleAudit=new RoleAudit(role,prevRequestStatus,roleRequest.getRequestStatus(),roleRequest,reason);
        roleAuditRepository.save(roleAudit);
    }
}
