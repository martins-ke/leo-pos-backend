package com.leo.pos.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
	List<AuditEvent> findByShopIdOrderByOccurredAtDesc(String shopId);
	void deleteByResourceTypeAndResourceIdIn(String resourceType, List<String> resourceIds);
}
