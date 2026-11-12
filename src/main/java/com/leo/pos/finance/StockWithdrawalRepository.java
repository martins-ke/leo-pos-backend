package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockWithdrawalRepository extends JpaRepository<StockWithdrawal, String> {
	Page<StockWithdrawal> findByShopIdOrderByOccurredAtDesc(String shopId, Pageable pageable);

	@Query("select coalesce(sum(w.totalCost), 0) from StockWithdrawal w "
			+ "where w.shop.id = :shopId and w.occurredAt >= :start and w.occurredAt < :end")
	BigDecimal sumByShopAndOccurredAt(@Param("shopId") String shopId,
			@Param("start") Instant start, @Param("end") Instant end);
}
