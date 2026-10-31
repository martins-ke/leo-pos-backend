package com.leo.pos.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

public interface StockWithdrawalRepository extends JpaRepository<StockWithdrawal, String> {
	Page<StockWithdrawal> findByShopIdOrderByOccurredAtDesc(String shopId, Pageable pageable);
}
