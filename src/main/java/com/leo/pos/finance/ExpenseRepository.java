package com.leo.pos.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, String> {
	List<Expense> findByShopIdOrderByExpenseDateDescCreatedAtDesc(String shopId);
	Page<Expense> findByShopIdOrderByExpenseDateDescCreatedAtDesc(String shopId, Pageable pageable);

	@Query("select coalesce(sum(e.amount), 0) from Expense e where e.shop.id = :shopId "
			+ "and e.expenseDate between :start and :end")
	BigDecimal sumByShopAndPeriod(@Param("shopId") String shopId, @Param("start") LocalDate start,
			@Param("end") LocalDate end);

	@Query("select coalesce(sum(e.amount), 0) from Expense e where e.shop.id = :shopId")
	BigDecimal sumByShopId(@Param("shopId") String shopId);

	@Query("select coalesce(sum(e.amount), 0) from Expense e "
			+ "where e.shop.id = :shopId and e.createdAt >= :createdAt")
	BigDecimal sumByShopIdSince(@Param("shopId") String shopId, @Param("createdAt") Instant createdAt);
}
