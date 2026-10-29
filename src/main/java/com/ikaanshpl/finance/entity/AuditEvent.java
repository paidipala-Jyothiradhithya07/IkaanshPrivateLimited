package com.ikaanshpl.finance.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events", indexes = @Index(name = "idx_audit_user_time", columnList = "user_id, created_at"))
public class AuditEvent {
	 @Id 
	 @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    @Column(name = "user_id")
	    private Long userId;

	    @Column(name = "entity_type", nullable = false, length = 50)
	    private String entityType;

	    @Column(name = "entity_id")
	    private Long entityId;

	    @Column(nullable = false, length = 30)
	    private String action;

	    @Column(length = 2000)
	    private String details;

	    @Column(name = "created_at", nullable = false)
	    private Instant createdAt = Instant.now();

		public AuditEvent(Long id, Long userId, String entityType, Long entityId, String action, String details,
				Instant createdAt) {
			super();
			this.id = id;
			this.userId = userId;
			this.entityType = entityType;
			this.entityId = entityId;
			this.action = action;
			this.details = details;
			this.createdAt = createdAt;
		}

		public AuditEvent() {
			super();
			// TODO Auto-generated constructor stub
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public String getEntityType() {
			return entityType;
		}

		public void setEntityType(String entityType) {
			this.entityType = entityType;
		}

		public Long getEntityId() {
			return entityId;
		}

		public void setEntityId(Long entityId) {
			this.entityId = entityId;
		}

		public String getAction() {
			return action;
		}

		public void setAction(String action) {
			this.action = action;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}

		public Instant getCreatedAt() {
			return createdAt;
		}

		public void setCreatedAt(Instant createdAt) {
			this.createdAt = createdAt;
		}
	    
	}

