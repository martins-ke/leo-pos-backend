package com.leo.pos.finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface ShopAccountRepository extends JpaRepository<ShopAccount, String> {
	Optional<ShopAccount> findByShopId(String shopId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from ShopAccount a where a.shop.id = :shopId")
	Optional<ShopAccount> findByShopIdForUpdate(@Param("shopId") String shopId);
}
