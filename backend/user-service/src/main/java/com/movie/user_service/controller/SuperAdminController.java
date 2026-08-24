package com.movie.user_service.controller;

import com.movie.user_service.dto.RoleDto;
import com.movie.user_service.service.RoleRequestService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/super")
@RequiredArgsConstructor
public class SuperAdminController {
    
    private final RoleRequestService roleRequestService;


    @PostMapping("/approve")
    public ResponseEntity<?> elevatePermissions(@RequestBody RoleDto roleDto){
        roleRequestService.elevate(roleDto);
        return ResponseEntity.status(HttpStatus.OK).body("%s Permissions approved successfully for userId: %d".formatted(roleDto.getRoleName(),roleDto.getUserId()));
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectPermissions(@RequestBody RoleDto roleDto){
        roleRequestService.reject(roleDto);
        return ResponseEntity.status(HttpStatus.OK).body("%s Permissions rejectted successfully for userId: %d".formatted(roleDto.getRoleName(),roleDto.getUserId()));
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revokePermissions(@RequestBody RoleDto roleDto){
        roleRequestService.revoke(roleDto);
        return ResponseEntity.status(HttpStatus.OK).body("%s Permissions revoked successfully for userId: %d".formatted(roleDto.getRoleName(),roleDto.getUserId()));
    }

    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivatePermissions(@RequestBody RoleDto roleDto){
        roleRequestService.reactivate(roleDto);
        return ResponseEntity.status(HttpStatus.OK).body("%s Permissions revoked successfully for userId: %d".formatted(roleDto.getRoleName(),roleDto.getUserId()));
    }
}
