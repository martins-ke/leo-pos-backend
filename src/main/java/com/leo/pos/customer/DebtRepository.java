package com.leo.pos.customer;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, String> {
	Page<Debt> findByCustomerShopIdAndStatusOrderByCreatedAtDesc(String shopId, String status, Pageable pageable);
	Page<Debt> findByCustomerShopIdOrderByCreatedAtDesc(String shopId, Pageable pageable);
	boolean existsByCustomerId(String customerId);
	List<Debt> findByStatusAndPurgeAtLessThanEqual(String status, Instant purgeAt, Pageable pageable);
}
