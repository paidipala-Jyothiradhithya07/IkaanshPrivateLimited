package com.ikaanshpl.finance.audit;

import org.aspectj.lang.annotation.AfterReturning;


import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.ikaanshpl.finance.security.AuthUser;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
@Component
public class AuditAspect {
	private static final Logger log =
            LoggerFactory.getLogger(AuditAspect.class);
	private static final int MAX_DETAILS = 1900;
    private final AuditWriter writer;
  
    public AuditAspect(AuditWriter writer) {
		super();
		this.writer = writer;
	}

	@AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void record(Audited audited, Object result) {
        try {
            Long entityId = (result instanceof Auditable a) ? a.auditEntityId() : null;
            String details = String.valueOf(result);
            if (details.length() > MAX_DETAILS) details = details.substring(0, MAX_DETAILS);
            writer.write(currentUserId(), audited.entity(), entityId, audited.action(), details);
        } catch (RuntimeException e) {
            // An audit failure must not undo a business operation that already succeeded.
        	log.error(
        		    "Failed to write audit event for {}/{}",
        		    audited.entity(),
        		    audited.action(),
        		    e
        		);
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getPrincipal() instanceof AuthUser u) ? u.id() : null;
    }

}
