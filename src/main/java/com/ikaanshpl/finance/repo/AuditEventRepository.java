package com.ikaanshpl.finance.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ikaanshpl.finance.entity.AuditEvent;


public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByUserId(Long userId, Pageable pageable);
}
