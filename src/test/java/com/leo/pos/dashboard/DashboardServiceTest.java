package com.leo.pos.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.finance.ExpenseRepository;
import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleItem;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.staff.User;

class DashboardServiceTest {

	@Test
	void calculatesGrossProfitUsingSaleTimeUnitCost() {
		SaleRepository saleRepo = mock(SaleRepository.class);
		ProductRepository productRepo = mock(ProductRepository.class);
		Product product = new Product();
		product.setBuyingPrice(new BigDecimal("9.00"));
		Sale sale = new Sale("sale-1", "receipt-1", new BigDecimal("20.00"), new BigDecimal("20.00"),
				BigDecimal.ZERO, "CASH", null, null);
		sale.addItem(new SaleItem(product, "Product", 2, new BigDecimal("10.00"),
				new BigDecimal("20.00"), new BigDecimal("5.00")));
		User actor = new User();
		actor.setId("owner-1");
		Shop shop = new Shop();
		shop.setId("shop-1");
		shop.setOwnerId(actor);
		ShopRepository shopRepo = mock(ShopRepository.class);
		when(shopRepo.findByUserId("owner-1")).thenReturn(List.of(shop));
		when(saleRepo.findByShopIdOrderByCreatedAtDesc("shop-1")).thenReturn(List.of(sale));
		when(productRepo.findByShopId("shop-1")).thenReturn(List.of());
		ExpenseRepository expenseRepo = mock(ExpenseRepository.class);
		when(expenseRepo.sumByShopAndPeriod(org.mockito.ArgumentMatchers.eq("shop-1"),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new BigDecimal("3.00"), new BigDecimal("5.00"));
		DashboardSummaryResponse summary = new DashboardService(saleRepo, productRepo,
				shopRepo, mock(ShopAccessService.class), expenseRepo).getSummary(null, actor);

		assertEquals(0, new BigDecimal("10.00").compareTo(summary.todayGrossProfit()));
		assertEquals(0, new BigDecimal("10.00").compareTo(summary.monthGrossProfit()));
		assertEquals(0, new BigDecimal("7.00").compareTo(summary.todayNetProfit()));
		assertEquals(0, new BigDecimal("5.00").compareTo(summary.monthNetProfit()));
		assertEquals(0, new BigDecimal("20.00").compareTo(summary.todayCashCollected()));
	}
}
