package com.movie.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.movie.user_service.entity.RoleAudit;

public interface RoleAuditRepository extends JpaRepository<RoleAudit,Long> {
    List<RoleAudit> findByRoleRequestIdOrderByCreatedAtAsc(Long roleRequestId);
}
