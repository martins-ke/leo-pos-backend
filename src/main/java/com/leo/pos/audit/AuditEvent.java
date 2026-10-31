package com.leo.pos.audit;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

	@Id
	private String id;
	private String shopId;
	@Column(nullable = false, updatable = false)
	private String actorId;
	@Column(nullable = false, updatable = false)
	private String actorName;
	@Column(nullable = false, updatable = false)
	private String action;
	@Column(nullable = false, updatable = false)
	private String resourceType;
	@Column(nullable = false, updatable = false)
	private String resourceId;
	@Column(length = 1000, updatable = false)
	private String details;
	@CreationTimestamp
	@Column(updatable = false)
	private Instant occurredAt;

	protected AuditEvent() {}

	public AuditEvent(String id, String shopId, String actorId, String actorName,
			String action, String resourceType, String resourceId, String details) {
		this.id = id;
		this.shopId = shopId;
		this.actorId = actorId;
		this.actorName = actorName;
		this.action = action;
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.details = details;
	}

	public String getId() { return id; }
	public String getShopId() { return shopId; }
	public String getActorId() { return actorId; }
	public String getActorName() { return actorName; }
	public String getAction() { return action; }
	public String getResourceType() { return resourceType; }
	public String getResourceId() { return resourceId; }
	public String getDetails() { return details; }
	public Instant getOccurredAt() { return occurredAt; }
}
