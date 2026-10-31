package com.leo.pos.sale;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.leo.pos.customer.CustomerSalesSummary;

public interface SaleRepository extends JpaRepository<Sale, String> {

	List<Sale> findAllByOrderByCreatedAtDesc();

	List<Sale> findByShopIdOrderByCreatedAtDesc(String shopId);

	List<Sale> findByCustomerIdOrderByCreatedAtDesc(String customerId);

	long countByCustomerId(String customerId);

	@Query("select coalesce(sum(s.grandTotal), 0) from Sale s where s.customer.id = :customerId")
	BigDecimal sumGrandTotalByCustomerId(@Param("customerId") String customerId);

	@Query("select s.customer.id as customerId, count(s) as salesCount, "
			+ "coalesce(sum(s.grandTotal), 0) as salesTotal from Sale s "
			+ "where s.customer.id in :customerIds group by s.customer.id")
	List<CustomerSalesSummary> summarizeSalesByCustomerIdIn(@Param("customerIds") List<String> customerIds);

	@Modifying
	@Query("update Sale s set s.customer = null where s.customer.id = :customerId")
	void anonymizeCustomerSales(@Param("customerId") String customerId);

	List<Sale> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

	List<Sale> findTop10ByOrderByCreatedAtDesc();

	long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

	@Query("select coalesce(sum(coalesce(i.subtotal, i.unitPrice * i.quantity) "
			+ "- coalesce(i.unitCost, p.buyingPrice, 0) * i.quantity), 0) "
			+ "from SaleItem i join i.sale s left join i.product p where s.shop.id = :shopId")
	BigDecimal sumGrossProfitByShopId(@Param("shopId") String shopId);

	@Query("select coalesce(sum(coalesce(i.subtotal, i.unitPrice * i.quantity) "
			+ "- coalesce(i.unitCost, p.buyingPrice, 0) * i.quantity), 0) "
			+ "from SaleItem i join i.sale s left join i.product p "
			+ "where s.shop.id = :shopId and s.createdAt >= :createdAt")
	BigDecimal sumGrossProfitByShopIdSince(
			@Param("shopId") String shopId, @Param("createdAt") LocalDateTime createdAt);
}
