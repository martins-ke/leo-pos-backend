package com.leo.pos.audit;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.leo.pos.staff.User;

@Service
public class AuditService {

	private final AuditEventRepository repository;

	public AuditService(AuditEventRepository repository) {
		this.repository = repository;
	}

	public void record(String shopId, User actor, String action, String resourceType, String resourceId, String details) {
		repository.save(new AuditEvent(
				UUID.randomUUID().toString(),
				shopId,
				actor.getId(),
				actor.getUsername(),
				action,
				resourceType,
				resourceId,
				details
		));
	}
}
