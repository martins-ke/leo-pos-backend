package com.leo.pos.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.leo.pos.product.Product;
import com.leo.pos.product.ProductRepository;
import com.leo.pos.security.ShopAccessService;
import com.leo.pos.shop.Shop;
import com.leo.pos.shop.ShopRepository;
import com.leo.pos.sale.Sale;
import com.leo.pos.sale.SaleItem;
import com.leo.pos.sale.SaleRepository;
import com.leo.pos.staff.User;

@Service
public class DashboardService {

	private final SaleRepository saleRepo;
	private final ProductRepository productRepo;
	private final ShopRepository shopRepo;
	private final ShopAccessService shopAccessService;

	public DashboardService(SaleRepository saleRepo, ProductRepository productRepo,
			ShopRepository shopRepo, ShopAccessService shopAccessService) {
		this.saleRepo = saleRepo;
		this.productRepo = productRepo;
		this.shopRepo = shopRepo;
		this.shopAccessService = shopAccessService;
	}

	public DashboardSummaryResponse getSummary(String shopId, User actor) {
		LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
		LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
		LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

		List<Shop> shops;
		if (shopId != null && !shopId.isBlank() && !"all".equalsIgnoreCase(shopId)) {
			shops = List.of(shopAccessService.requireOwnerShop(shopId, actor));
		} else {
			shops = shopRepo.findByUserId(actor.getId());
		}
		List<Sale> allSales = shops.stream()
				.flatMap(shop -> saleRepo.findByShopIdOrderByCreatedAtDesc(shop.getId()).stream()).toList();

		BigDecimal todayRevenue = BigDecimal.ZERO;
		BigDecimal todayGrossProfit = BigDecimal.ZERO;
		BigDecimal monthRevenue = BigDecimal.ZERO;
		BigDecimal monthGrossProfit = BigDecimal.ZERO;
		long totalSalesToday = 0;

		for (Sale s : allSales) {
			if (s.getCreatedAt() != null) {
				if (!s.getCreatedAt().isBefore(startOfDay) && !s.getCreatedAt().isAfter(endOfDay)) {
					todayRevenue = todayRevenue.add(s.getGrandTotal() != null ? s.getGrandTotal() : BigDecimal.ZERO);
					todayGrossProfit = todayGrossProfit.add(calculateGrossProfit(s));
					totalSalesToday++;
				}
				if (!s.getCreatedAt().isBefore(startOfMonth)) {
					monthRevenue = monthRevenue.add(s.getGrandTotal() != null ? s.getGrandTotal() : BigDecimal.ZERO);
					monthGrossProfit = monthGrossProfit.add(calculateGrossProfit(s));
				}
			}
		}

		List<Product> allProducts = shops.stream()
				.flatMap(shop -> productRepo.findByShopId(shop.getId()).stream()).toList();

		long totalProducts = allProducts.size();
		List<Product> lowStockProducts = new ArrayList<>();
		for (Product p : allProducts) {
			if (p.isActive() && p.getQuantity() <= p.getMinStockLevel()) {
				lowStockProducts.add(p);
			}
		}

		List<RecentSaleDto> recentSales = new ArrayList<>();
		int count = 0;
		for (Sale s : allSales) {
			if (count++ >= 10) break;
			String shopName = s.getShop() != null ? s.getShop().getName() : "Main Outlet";
			String cashierName = s.getCashier() != null ? s.getCashier().getUsername() : "Cashier";
			recentSales.add(new RecentSaleDto(
					s.getId(),
					s.getReceiptNumber(),
					shopName,
					cashierName,
					s.getGrandTotal(),
					s.getPaymentMethod(),
					s.getCreatedAt()
			));
		}

		return new DashboardSummaryResponse(
				todayRevenue,
				todayGrossProfit,
				monthRevenue,
				monthGrossProfit,
				totalSalesToday,
				totalProducts,
				lowStockProducts.size(),
				recentSales,
				lowStockProducts
		);
	}

	private BigDecimal calculateGrossProfit(Sale sale) {
		BigDecimal grossProfit = BigDecimal.ZERO;
		for (SaleItem item : sale.getItems()) {
			BigDecimal itemRevenue = item.getSubtotal() != null
					? item.getSubtotal()
					: item.getUnitPrice() != null
							? item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
							: BigDecimal.ZERO;
			BigDecimal unitCost = item.getUnitCost() != null
					? item.getUnitCost()
					: item.getProduct() != null ? item.getProduct().getBuyingPrice() : null;
			BigDecimal itemCost = unitCost != null
					? unitCost.multiply(BigDecimal.valueOf(item.getQuantity()))
					: BigDecimal.ZERO;
			grossProfit = grossProfit.add(itemRevenue.subtract(itemCost));
		}
		return grossProfit.setScale(2, RoundingMode.HALF_UP);
	}
}
