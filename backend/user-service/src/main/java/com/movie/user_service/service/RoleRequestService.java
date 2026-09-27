package com.movie.user_service.service;

import com.movie.user_service.repository.RoleAuditRepository;
import com.movie.user_service.repository.RoleRequestRepository;

import java.util.Set;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.dto.RoleAuditDto;
import com.movie.user_service.dto.RoleRequestSummaryDto;
import com.movie.user_service.dto.RoleRequestProgressDto;
import com.movie.user_service.dto.RoleAuditStatusDto;
import com.movie.user_service.entity.RoleRequest;
import com.movie.user_service.entity.RoleStatus;
import com.movie.user_service.entity.RequestStatus;
import com.movie.user_service.entity.Role;
import com.movie.user_service.entity.RoleAudit;
import com.movie.user_service.entity.User;
import com.movie.user_service.exceptions.AlreadyExistsException;
import com.movie.user_service.exceptions.InvalidRoleRequestStateException;
import com.movie.user_service.exceptions.ResourceNotFoundException;
import com.movie.user_service.exceptions.UnverifiedAccountException;
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
    private final Set<String> allowedRoles = Set.of("ROLE_ADMIN", "ROLE_THEATER_ADMIN");

    @Transactional
    public void elevate(Long requestId, String reason) {
        RoleRequest roleRequest = findRequest(requestId);
        requireCurrentStatus(roleRequest, RequestStatus.PENDING);
        roleRequest.getUser().getRoles().add(roleRequest.getRequestedRole());
        changeRequestStatus(RequestStatus.APPROVED, RoleStatus.ACTIVE, roleRequest, reason);
    }
@Transactional
    public void reject(Long requestId, String reason) {
        RoleRequest roleRequest = findRequest(requestId);
        requireCurrentStatus(roleRequest, RequestStatus.PENDING);
        changeRequestStatus(RequestStatus.REJECTED, null, roleRequest, reason);
    }
@Transactional
    public void revoke(Long requestId, String reason) {
        RoleRequest roleRequest = findRequest(requestId);
        requireCurrentStatus(roleRequest, RequestStatus.APPROVED, RequestStatus.REACTIVATED);
        roleRequest.getUser().getRoles().remove(roleRequest.getRequestedRole());
        changeRequestStatus(RequestStatus.REVOKED, RoleStatus.REVOKED, roleRequest, reason);
    }

@Transactional
    public Long request(String email,RoleDto roleDto) {
        if(!allowedRoles.contains(roleDto.getRoleName())) {
            throw new IllegalArgumentException("The requested role cannot be upgraded through this endpoint.");
        }
        
        User user = userRepository.findByEmail(email).orElseThrow(()-> new UsernameNotFoundException("No User found"));
        if (!user.isEmailVerified()) {
            throw new UnverifiedAccountException("Verify your email address before requesting a role upgrade.");
        }
        Role requestedRole = roleService.getRole(roleDto.getRoleName());
        if (user.getRoles().stream().anyMatch(role -> role.getRoleName().equals(requestedRole.getRoleName()))) {
            throw new AlreadyExistsException("User already has " + requestedRole.getRoleName());
        }
        if (roleRequestRepository.existsByUserAndRequestStatusAndRequestedRole(
                user,
                RequestStatus.PENDING,requestedRole)) {
            throw new AlreadyExistsException(requestedRole + " Request Already exists");
        }

        

        RoleRequest roleRequest = new RoleRequest();
        roleRequest.setRequestedRole(requestedRole);
        roleRequest.setUser(user);
        roleRequestRepository.save(roleRequest);
        roleAuditRepository.save(new RoleAudit(requestedRole, null, RequestStatus.PENDING, roleRequest, roleDto.getReason()));
        return roleRequest.getId();
    }

@Transactional
    public void reactivate(Long requestId, String reason) {
        RoleRequest roleRequest = findRequest(requestId);
        requireCurrentStatus(roleRequest, RequestStatus.REVOKED);
        roleRequest.getUser().getRoles().add(roleRequest.getRequestedRole());
        changeRequestStatus(RequestStatus.REACTIVATED, RoleStatus.ACTIVE, roleRequest, reason);
    }

@Transactional
    private void changeRequestStatus(RequestStatus currRequestStatus,
            RoleStatus roleStatus, RoleRequest roleRequest, String reason) {
        
        RequestStatus prevRequestStatus=roleRequest.getRequestStatus();

        roleRequest.setRequestStatus(currRequestStatus);
        roleRequest.setRoleStatus(roleStatus);
        
        RoleAudit roleAudit=new RoleAudit(roleRequest.getRequestedRole(), prevRequestStatus,
                roleRequest.getRequestStatus(), roleRequest, reason);
        roleAuditRepository.save(roleAudit);
    }

    private RoleRequest findRequest(Long requestId) {
        return roleRequestRepository.findByIdWithUserAndRequestedRole(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Role request not found"));
    }

    private void requireCurrentStatus(RoleRequest roleRequest, RequestStatus... expectedStatuses) {
        for (RequestStatus expectedStatus : expectedStatuses) {
            if (roleRequest.getRequestStatus() == expectedStatus) {
                return;
            }
        }
        throw new InvalidRoleRequestStateException(
                "Role request " + roleRequest.getId() + " is " + roleRequest.getRequestStatus());
    }

    @Transactional
    public java.util.List<RoleRequestSummaryDto> pendingRequests() {
        return roleRequestRepository.findAllByRequestStatusWithUserAndRequestedRole(RequestStatus.PENDING).stream()
                .map(request -> new RoleRequestSummaryDto(
                        request.getId(), request.getUser().getUserName(), request.getUser().getEmail(),
                        request.getRequestedRole().getRoleName(), request.getRequestStatus(), request.getRoleStatus(),
                        request.getCreatedAt()))
                .toList();
    }

    @Transactional
    public java.util.List<RoleAuditDto> auditTrail(Long requestId) {
        findRequest(requestId);
        return roleAuditRepository.findByRoleRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(audit -> new RoleAuditDto(audit.getId(), audit.getPrevRequestStatus(),
                        audit.getCurrRequestStatus(), audit.getReason(), audit.getCreatedBy(), audit.getCreatedAt()))
                .toList();
    }

    /** Returns only the authenticated user's requests and their user-safe audit history. */
    @Transactional
    public java.util.List<RoleRequestProgressDto> myRequestProgress(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return roleRequestRepository.findAllByUserWithRequestedRoleOrderByCreatedAtDesc(user).stream()
                .map(request -> new RoleRequestProgressDto(
                        request.getId(), request.getRequestedRole().getRoleName(), request.getRequestStatus(),
                        request.getRoleStatus(), request.getCreatedAt(),
                        roleAuditRepository.findByRoleRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                                .map(audit -> new RoleAuditStatusDto(audit.getPrevRequestStatus(),
                                        audit.getCurrRequestStatus(), audit.getReason(), audit.getCreatedAt()))
                                .toList()))
                .toList();
    }
}
