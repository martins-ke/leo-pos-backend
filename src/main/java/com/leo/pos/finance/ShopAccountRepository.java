package com.leo.pos.finance;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface ShopAccountRepository extends JpaRepository<ShopAccount, String> {
	Optional<ShopAccount> findByShopId(String shopId);

	@Query(value = "SELECT * FROM shop_accounts WHERE shop_id = :shopId FOR UPDATE", nativeQuery = true)
	Optional<ShopAccount> findByShopIdForUpdate(@Param("shopId") String shopId);
}
