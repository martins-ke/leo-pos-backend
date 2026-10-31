package com.leo.pos.finance;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, String> {
	Page<AccountTransaction> findByAccountIdOrderByOccurredAtDesc(String accountId, Pageable pageable);

	@Query("select coalesce(sum(case when t.direction = 'IN' then t.amount else -t.amount end), 0) "
			+ "from AccountTransaction t where t.account.id = :accountId")
	BigDecimal calculateBalance(@Param("accountId") String accountId);

	List<AccountTransaction> findByAccountShopIdAndTypeAndOccurredAtAfter(
			String shopId, String type, java.time.Instant after);

	@Query("select coalesce(sum(t.amount), 0) from AccountTransaction t "
			+ "where t.account.shop.id = :shopId and t.type = 'OWNER_DRAWING' and t.direction = 'OUT'")
	BigDecimal calculateTotalDrawingsByShopId(@Param("shopId") String shopId);

	@Query("select coalesce(sum(case when t.direction = 'IN' then t.amount else -t.amount end), 0) "
			+ "from AccountTransaction t where t.account.shop.id = :shopId and t.type = 'OWNER_DRAWING' "
			+ "and t.occurredAt >= :start and t.occurredAt < :end")
	BigDecimal calculateDrawingsByPeriod(@Param("shopId") String shopId,
			@Param("start") java.time.Instant start, @Param("end") java.time.Instant end);
}
