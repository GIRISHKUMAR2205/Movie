package com.movie.user_service.entity;



import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "role_requests")
@Getter
@Setter
@NoArgsConstructor
public class RoleRequest extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status",nullable = false)
    private RequestStatus requestStatus=RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_status",nullable = true)
    private RoleStatus roleStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_role_id", nullable = false)
    private Role requestedRole;

}
