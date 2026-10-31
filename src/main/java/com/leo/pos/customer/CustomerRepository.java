package com.leo.pos.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
	Page<Customer> findByShopIdOrderByNameAsc(String shopId, Pageable pageable);
}
