package com.leo.pos.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockPurchaseRepository extends JpaRepository<StockPurchase, String> {
	Page<StockPurchase> findByShopIdOrderByOccurredAtDesc(String shopId, Pageable pageable);
}
