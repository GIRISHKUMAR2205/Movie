package com.movie.user_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "role_audit")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoleAudit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curr_role_id", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "prev_request_status")
    private RequestStatus prevRequestStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "curr_request_status",nullable = false)
    private RequestStatus currRequestStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_request_id", nullable = false)
    private RoleRequest roleRequest;

    @Column(name = "reason", length = 500)
    private String reason;
}
