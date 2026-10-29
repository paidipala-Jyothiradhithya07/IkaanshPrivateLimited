package com.ikaanshpl.finance.audit;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ikaanshpl.finance.entity.AuditEvent;
import com.ikaanshpl.finance.repo.AuditEventRepository;

@Component
public class AuditWriter {
	private final AuditEventRepository repo =null;
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(Long userId, String entityType, Long entityId, String action, String details) {
        AuditEvent e = new AuditEvent();
        e.setUserId(userId);
        e.setEntityType(entityType);
        e.setEntityId(entityId);
        e.setAction(action);
        e.setDetails(details);
        repo.save(e);
}
}