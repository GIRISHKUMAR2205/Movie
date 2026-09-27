package com.movie.user_service.controller;

import com.movie.user_service.dto.RoleDecisionDto;
import com.movie.user_service.dto.GatewayResponseDto;
import com.movie.user_service.dto.RoleRequestSummaryDto;
import com.movie.user_service.dto.RoleAuditDto;
import com.movie.user_service.service.RoleRequestService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/super")
@RequiredArgsConstructor
public class SuperAdminController {
    
    private final RoleRequestService roleRequestService;

    @GetMapping("/role-requests")
    public ResponseEntity<GatewayResponseDto<List<RoleRequestSummaryDto>>> pendingRoleRequests() {
        return ResponseEntity.ok(GatewayResponseDto.success(HttpStatus.OK, "ROLE_REQUESTS_RETRIEVED",
                "Pending role requests retrieved successfully.", roleRequestService.pendingRequests()));
    }

    @GetMapping("/role-requests/{requestId}/audit")
    public ResponseEntity<GatewayResponseDto<List<RoleAuditDto>>> roleAudit(@PathVariable Long requestId) {
        return ResponseEntity.ok(GatewayResponseDto.success(HttpStatus.OK, "ROLE_AUDIT_RETRIEVED",
                "Role request audit retrieved successfully.", roleRequestService.auditTrail(requestId)));
    }


    @PostMapping("/role-requests/{requestId}/approve")
    public ResponseEntity<GatewayResponseDto<Void>> elevatePermissions(
            @PathVariable Long requestId, @Valid @RequestBody RoleDecisionDto decision){
        roleRequestService.elevate(requestId, decision.reason());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "ROLE_APPROVED", "Role approved successfully.", null));
    }

    @PostMapping("/role-requests/{requestId}/reject")
    public ResponseEntity<GatewayResponseDto<Void>> rejectPermissions(
            @PathVariable Long requestId, @Valid @RequestBody RoleDecisionDto decision){
        roleRequestService.reject(requestId, decision.reason());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "ROLE_REJECTED", "Role rejected successfully.", null));
    }

    @PostMapping("/role-requests/{requestId}/revoke")
    public ResponseEntity<GatewayResponseDto<Void>> revokePermissions(
            @PathVariable Long requestId, @Valid @RequestBody RoleDecisionDto decision){
        roleRequestService.revoke(requestId, decision.reason());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "ROLE_REVOKED", "Role revoked successfully.", null));
    }

    @PostMapping("/role-requests/{requestId}/reactivate")
    public ResponseEntity<GatewayResponseDto<Void>> reactivatePermissions(
            @PathVariable Long requestId, @Valid @RequestBody RoleDecisionDto decision){
        roleRequestService.reactivate(requestId, decision.reason());
        return ResponseEntity.ok(GatewayResponseDto.success(
                HttpStatus.OK, "ROLE_REACTIVATED", "Role reactivated successfully.", null));
    }

}
